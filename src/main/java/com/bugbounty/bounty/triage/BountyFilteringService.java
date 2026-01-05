package com.bugbounty.bounty.triage;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.repository.entity.RepositoryEntity;
import com.bugbounty.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BountyFilteringService {

    private static final double DEFAULT_MIN_CONFIDENCE = 0.6;
    private static final int DEFAULT_MAX_TIME_MINUTES = 60;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final RepositoryRepository repositoryRepository;

    @Value("${app.bounty.triage.supported-languages:Java,TypeScript,JavaScript,Python}")
    private String supportedLanguagesConfig;

    @Value("${app.bounty.triage.max-complexity:simple}")
    private String maxComplexity; // simple, moderate, complex

    @Value("${app.bounty.triage.max-time-minutes:60}")
    private int maxTimeMinutes;

    @Value("${app.bounty.triage.min-confidence:0.9}")
    private double minConfidence;

    @Value("${app.bounty.triage.max-bounty-amount:200}")
    private String maxBountyAmountStr;

    /**
     * Get set of supported languages from configuration.
     */
    private Set<String> getSupportedLanguages() {
        return Arrays.stream(supportedLanguagesConfig.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
     * Get maximum bounty amount from configuration.
     * Higher bounties usually indicate complexity.
     */
    private BigDecimal getMaxBountyAmount() {
        try {
            return new BigDecimal(maxBountyAmountStr);
        } catch (Exception e) {
            log.warn("Invalid max-bounty-amount config '{}', using default 200", maxBountyAmountStr, e);
            return new BigDecimal("200");
        }
    }

    public FilterResult shouldProcess(Bounty bounty) {
        return shouldProcess(bounty, minConfidence, maxTimeMinutes);
    }

    public FilterResult shouldProcess(Bounty bounty, double minConfidence) {
        return shouldProcess(bounty, minConfidence, maxTimeMinutes);
    }

    public FilterResult shouldProcess(Bounty bounty, double minConfidence, int maxTimeMinutes) {
        try {
            log.debug("Filtering bounty: {} - {}", bounty.getIssueId(), bounty.getTitle());

            // Step 1: Pre-filter by language (fast, no LLM call)
            FilterResult languageCheck = checkLanguage(bounty);
            if (!languageCheck.shouldProcess()) {
                log.debug("Bounty {} rejected at language check: {}", bounty.getIssueId(), languageCheck.reason());
                return languageCheck;
            }

            // Step 1.5: Pre-filter by bounty amount (fast, no LLM call)
            // Higher bounties usually indicate complexity - reject them early
            BigDecimal maxAmount = getMaxBountyAmount();
            if (bounty.getAmount() != null && bounty.getAmount().compareTo(maxAmount) > 0) {
                log.debug("Bounty {} rejected: amount {} exceeds maximum {}", 
                        bounty.getIssueId(), bounty.getAmount(), maxAmount);
                return new FilterResult(false, 0.0, 0, 
                        String.format("Bounty amount %s exceeds maximum %s (higher amounts usually indicate complexity)", 
                                bounty.getAmount(), maxAmount));
            }

            // Step 2: LLM-based complexity and feasibility analysis
            String promptText = buildPrompt(bounty, languageCheck);
            Prompt aiPrompt = new Prompt(new UserMessage(promptText));

            ChatResponse response = chatClient.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();

            FilterResult result = parseResponse(content);

            // Apply thresholds
            if (result.confidence() < minConfidence) {
                log.debug("Bounty {} rejected: confidence {} below threshold {}", 
                        bounty.getIssueId(), result.confidence(), minConfidence);
                return new FilterResult(false, result.confidence(), result.estimatedTimeMinutes(), 
                        result.reason() + " (confidence too low)");
            }

            if (result.estimatedTimeMinutes() > maxTimeMinutes) {
                log.debug("Bounty {} rejected: estimated time {} exceeds threshold {}", 
                        bounty.getIssueId(), result.estimatedTimeMinutes(), maxTimeMinutes);
                return new FilterResult(false, result.confidence(), result.estimatedTimeMinutes(), 
                        result.reason() + " (time estimate too high)");
            }

            log.info("Bounty {} accepted: confidence={}, time={}min, language={}", 
                    bounty.getIssueId(), result.confidence(), result.estimatedTimeMinutes(), 
                    getRepositoryLanguage(bounty));
            return result;

        } catch (Exception e) {
            log.error("Error filtering bounty: {}", bounty.getIssueId(), e);
            // Fail-safe: reject on error
            return new FilterResult(false, 0.0, 0, "Error during filtering: " + e.getMessage());
        }
    }

    /**
     * Pre-filter by language - reject if repository language is not in supported languages.
     * This is a fast check that doesn't require LLM calls.
     */
    private FilterResult checkLanguage(Bounty bounty) {
        String repositoryLanguage = getRepositoryLanguage(bounty);
        
        if (repositoryLanguage == null || repositoryLanguage.isEmpty()) {
            // If we can't determine language, let LLM decide (might be a new repo)
            log.debug("Could not determine repository language for bounty {}", bounty.getIssueId());
            return new FilterResult(true, 0.5, 0, "Language unknown - will check in LLM analysis");
        }

        Set<String> supportedLanguages = getSupportedLanguages();
        String languageLower = repositoryLanguage.toLowerCase();

        // Check exact match and common variations
        boolean isSupported = supportedLanguages.contains(languageLower) ||
                supportedLanguages.contains(normalizeLanguage(languageLower));

        if (!isSupported) {
            log.info("Bounty {} rejected: language '{}' not in supported languages: {}", 
                    bounty.getIssueId(), repositoryLanguage, supportedLanguages);
            return new FilterResult(false, 0.0, 0, 
                    String.format("Language '%s' not supported. Supported: %s", 
                            repositoryLanguage, String.join(", ", supportedLanguages)));
        }

        return new FilterResult(true, 1.0, 0, "Language supported: " + repositoryLanguage);
    }

    /**
     * Normalize language names for matching (e.g., "TypeScript" -> "typescript", "JS" -> "javascript").
     */
    private String normalizeLanguage(String language) {
        String normalized = language.toLowerCase().trim();
        // Common aliases
        if (normalized.equals("js") || normalized.equals("jsx") || normalized.equals("tsx")) {
            return "javascript";
        }
        if (normalized.equals("ts")) {
            return "typescript";
        }
        if (normalized.equals("py")) {
            return "python";
        }
        return normalized;
    }

    /**
     * Get repository language from database or infer from repository URL/name.
     */
    private String getRepositoryLanguage(Bounty bounty) {
        if (bounty.getRepositoryUrl() == null) {
            return null;
        }

        // Try to get from database first
        Optional<RepositoryEntity> repoOpt = repositoryRepository.findByUrl(bounty.getRepositoryUrl());
        if (repoOpt.isPresent() && repoOpt.get().getLanguage() != null) {
            return repoOpt.get().getLanguage();
        }

        // Could infer from repository name or URL patterns, but for now return null
        return null;
    }

    private String buildPrompt(Bounty bounty, FilterResult languageCheck) {
        String repositoryLanguage = getRepositoryLanguage(bounty);
        String languageInfo = repositoryLanguage != null ? 
                String.format("Repository Language: %s (supported)", repositoryLanguage) : 
                "Repository Language: Unknown";
        
        return """
                Analyze this bug bounty and determine if it should be processed.
                
                CRITICAL: We are looking for SIMPLE, QUICK bugs that can be fixed fast for quick cash.
                BRUTAL REALITY CHECK: Most Algora bounties require full POCs, multiple PR iterations, 
                and deep codebase understanding. We need to REJECT 95%+ of bounties and only accept 
                truly trivial fixes that can be done in a single file with no POC required.
                
                Bounty Details:
                - Issue ID: {issueId}
                - Repository: {repositoryUrl}
                - {languageInfo}
                - Platform: {platform}
                - Amount: {amount} {currency}
                - Title: {title}
                - Description: {description}
                
                CRITICAL REJECTION CRITERIA (reject if ANY apply):
                1. **POC Required**: Mentions "proof", "exploit", "demonstration", "POC", "PoC", "proof of concept"
                2. **Security Bug**: Security vulnerabilities need POCs to prove
                3. **Multiple Files**: Requires changes in more than 1 file
                4. **Architecture**: Mentions "refactor", "architecture", "design pattern", "restructure"
                5. **Testing Required**: Needs new tests, test suites, or extensive testing
                6. **Documentation**: Requires documentation updates
                7. **Vague**: Description is unclear, incomplete, or missing steps
                8. **High Value**: Bounty > $300 usually indicates complexity
                9. **Performance**: Performance issues require profiling/benchmarking
                10. **Integration**: Requires understanding external systems or APIs
                11. **Multiple Iterations**: Any indication that feedback/iterations are expected
                12. **Complex Logic**: Involves algorithms, data structures, or complex business logic
                
                ACCEPT ONLY if ALL of these are true:
                1. **Single File**: Fix in exactly 1 file, no other files touched
                2. **Trivial Bug**: Obvious error (typo, wrong variable name, missing null check, wrong operator)
                3. **No POC**: Just a code fix, no proof/demonstration needed
                4. **Clear Description**: Issue has clear steps, expected vs actual behavior
                5. **Low Complexity**: Can understand the fix without reading other files
                6. **No Testing**: Fix doesn't require new tests (or tests already exist)
                7. **Low Value**: Bounty < $200 (simple fixes are usually lower value)
                8. **Non-Security**: Not a security vulnerability
                9. **Quick Fix**: Can be done in under {maxTime} minutes
                10. **High Confidence**: 90%+ confidence this is truly trivial
                
                BE BRUTAL: If there's ANY doubt, REJECT. Better to miss a bounty than waste time on complex ones.
                
                Respond with a JSON object:
                {{
                  "shouldProcess": true/false,
                  "confidence": 0.0-1.0,
                  "estimatedTimeMinutes": number,
                  "complexity": "simple|moderate|complex",
                  "reason": "brief explanation - be specific about why accepted/rejected"
                }}
                """.replace("{issueId}", bounty.getIssueId())
                .replace("{repositoryUrl}", bounty.getRepositoryUrl() != null ? bounty.getRepositoryUrl() : "N/A")
                .replace("{languageInfo}", languageInfo)
                .replace("{platform}", bounty.getPlatform())
                .replace("{amount}", bounty.getAmount() != null ? bounty.getAmount().toString() : "N/A")
                .replace("{currency}", bounty.getCurrency() != null ? bounty.getCurrency() : "USD")
                .replace("{title}", bounty.getTitle() != null ? bounty.getTitle() : "N/A")
                .replace("{description}", bounty.getDescription() != null ? bounty.getDescription() : "N/A")
                .replace("{maxTime}", String.valueOf(maxTimeMinutes));
    }

    private FilterResult parseResponse(String content) {
        try {
            // Extract JSON from response (might have markdown code blocks)
            String jsonContent = content.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
            }
            if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring(3);
            }
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            jsonContent = jsonContent.trim();

            JsonNode node = objectMapper.readTree(jsonContent);
            
            boolean shouldProcess = node.get("shouldProcess").asBoolean();
            double confidence = node.get("confidence").asDouble();
            int estimatedTime = node.get("estimatedTimeMinutes").asInt();
            String reason = node.has("reason") ? node.get("reason").asText() : "";
            
            // Additional complexity check
            if (node.has("complexity")) {
                String complexity = node.get("complexity").asText().toLowerCase();
                
                // Reject if complexity is too high
                if ("complex".equals(complexity) && !"complex".equals(maxComplexity.toLowerCase())) {
                    log.debug("Bounty rejected: complexity '{}' exceeds maximum '{}'", complexity, maxComplexity);
                    return new FilterResult(false, confidence, estimatedTime, 
                            reason + " (complexity too high: " + complexity + ")");
                }
                
                // Be more strict with moderate complexity
                if ("moderate".equals(complexity) && "simple".equals(maxComplexity.toLowerCase())) {
                    log.debug("Bounty rejected: moderate complexity exceeds simple threshold");
                    return new FilterResult(false, confidence, estimatedTime, 
                            reason + " (moderate complexity, only simple bugs accepted)");
                }
            }

            return new FilterResult(shouldProcess, confidence, estimatedTime, reason);
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", content, e);
            return new FilterResult(false, 0.0, 0, "Failed to parse LLM response");
        }
    }
}

