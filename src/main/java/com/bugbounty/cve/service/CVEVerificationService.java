package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.BugFinding;
import com.bugbounty.cve.entity.BugFindingEntity;
import com.bugbounty.cve.mapper.BugFindingMapper;
import com.bugbounty.cve.repository.BugFindingRepository;
import com.bugbounty.cve.repository.CVECatalogRepository;
import com.bugbounty.cve.entity.CodebaseIndexEntity;
import com.bugbounty.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

/**
 * Service for cross-LLM verification of CVE findings and fix generation.
 * Uses a second LLM to verify CVE presence and generate fixes,
 * then uses the first LLM to confirm the fix.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CVEVerificationService {
    
    private final ChatClient chatClient; // LLM client (used for both primary and secondary verification)
    
    private final BugFindingRepository bugFindingRepository;
    private final BugFindingMapper bugFindingMapper;
    private final CVECatalogRepository catalogRepository;
    private final CodebaseIndexService codebaseIndexService;
    private final RepositoryRepository repositoryRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Verify and process a bug finding through cross-LLM verification.
     * 
     * @param findingId Bug finding ID to verify
     * @return Updated bug finding
     */
    public Mono<BugFinding> verifyAndProcessBugFinding(java.util.UUID findingId) {
        return Mono.fromCallable(() -> {
            Optional<BugFindingEntity> entityOpt = bugFindingRepository.findById(findingId);
            if (entityOpt.isEmpty()) {
                throw new IllegalArgumentException("Bug finding not found: " + findingId);
            }
            
            BugFindingEntity entity = entityOpt.get();
            BugFinding finding = bugFindingMapper.toDomain(entity);
            
            log.info("Verifying bug finding {} for CVE {} in commit {}", 
                    findingId, finding.getCveId(), finding.getCommitId());
            
            // Step 1: Second LLM verifies CVE presence
            BugFinding verified = verifyCVEPresence(finding);
            
            if (verified.getPresenceConfidence() == null || verified.getPresenceConfidence() < 0.7) {
                log.warn("CVE verification failed or low confidence for finding {}", findingId);
                verified.setRequiresHumanReview(true);
                verified.setStatus(BugFinding.BugFindingStatus.HUMAN_REVIEW);
                updateBugFinding(verified);
                return verified;
            }
            
            // Step 2: Second LLM generates fix
            BugFinding withFix = generateFix(verified);
            
            // Step 3: First LLM confirms fix
            BugFinding confirmed = confirmFix(withFix);
            
            // Update status based on confidence
            if (confirmed.getFixConfidence() != null && confirmed.getFixConfidence() >= 0.8) {
                confirmed.setStatus(BugFinding.BugFindingStatus.FIX_CONFIRMED);
                confirmed.setRequiresHumanReview(false);
            } else if (confirmed.getFixConfidence() != null && confirmed.getFixConfidence() >= 0.6) {
                confirmed.setStatus(BugFinding.BugFindingStatus.FIX_GENERATED);
                confirmed.setRequiresHumanReview(true);
            } else {
                confirmed.setStatus(BugFinding.BugFindingStatus.HUMAN_REVIEW);
                confirmed.setRequiresHumanReview(true);
            }
            
            updateBugFinding(confirmed);
            log.info("Completed verification for bug finding {} - Status: {}, Fix Confidence: {}", 
                    findingId, confirmed.getStatus(), confirmed.getFixConfidence());
            
            return confirmed;
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    /**
     * Step 1: Second LLM verifies CVE presence.
     */
    private BugFinding verifyCVEPresence(BugFinding finding) {
        try {
            // Get CVE catalog entry
            var catalogOpt = catalogRepository.findByCveIdAndLanguage(
                    finding.getCveId(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            
            if (catalogOpt.isEmpty()) {
                log.warn("CVE catalog entry not found for {} in language", finding.getCveId());
                finding.setPresenceConfidence(0.0);
                return finding;
            }
            
            var catalog = catalogOpt.get();
            
            // Get codebase index
            Optional<CodebaseIndexEntity> indexOpt = codebaseIndexService.getIndex(
                    finding.getRepositoryUrl(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            String codebaseIndex = indexOpt.map(CodebaseIndexEntity::getIndexData).orElse("{}");
            
            // Build verification prompt
            String prompt = buildVerificationPrompt(finding, catalog, codebaseIndex);
            Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
            
            // Use LLM for verification (second verification)
            ChatResponse response = chatClient.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();
            
            // Parse response
            VerificationResult result = parseVerificationResponse(content);
            
            // Update finding
            finding.setPresenceConfidence(result.confidence);
            finding.setStatus(BugFinding.BugFindingStatus.VERIFIED);
            finding.setVerificationNotes(
                    (finding.getVerificationNotes() != null ? finding.getVerificationNotes() + "\n\n" : "") +
                    "Verification: " + result.notes);
            
            return finding;
            
        } catch (Exception e) {
            log.error("Error verifying CVE presence for finding {}", finding.getId(), e);
            finding.setPresenceConfidence(0.0);
            finding.setRequiresHumanReview(true);
            return finding;
        }
    }
    
    /**
     * Step 2: Second LLM generates fix code.
     */
    private BugFinding generateFix(BugFinding finding) {
        try {
            // Get CVE catalog entry
            var catalogOpt = catalogRepository.findByCveIdAndLanguage(
                    finding.getCveId(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            
            if (catalogOpt.isEmpty()) {
                log.warn("CVE catalog entry not found for fix generation");
                return finding;
            }
            
            var catalog = catalogOpt.get();
            
            // Get codebase index
            Optional<CodebaseIndexEntity> indexOpt = codebaseIndexService.getIndex(
                    finding.getRepositoryUrl(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            String codebaseIndex = indexOpt.map(CodebaseIndexEntity::getIndexData).orElse("{}");
            
            // Build fix generation prompt
            String prompt = buildFixGenerationPrompt(finding, catalog, codebaseIndex);
            Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
            
            // Use LLM for fix generation (second LLM step)
            ChatResponse response = chatClient.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();
            
            // Parse response
            FixResult result = parseFixResponse(content);
            
            // Update finding
            finding.setRecommendedFix(result.fixCode);
            finding.setStatus(BugFinding.BugFindingStatus.FIX_GENERATED);
            finding.setVerificationNotes(
                    (finding.getVerificationNotes() != null ? finding.getVerificationNotes() + "\n\n" : "") +
                    "Fix Generation: " + result.notes);
            
            return finding;
            
        } catch (Exception e) {
            log.error("Error generating fix for finding {}", finding.getId(), e);
            finding.setRequiresHumanReview(true);
            return finding;
        }
    }
    
    /**
     * Step 3: First LLM confirms fix correctness.
     */
    private BugFinding confirmFix(BugFinding finding) {
        try {
            // Get CVE catalog entry
            var catalogOpt = catalogRepository.findByCveIdAndLanguage(
                    finding.getCveId(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            
            if (catalogOpt.isEmpty()) {
                log.warn("CVE catalog entry not found for fix confirmation");
                return finding;
            }
            
            var catalog = catalogOpt.get();
            
            // Get codebase index
            Optional<CodebaseIndexEntity> indexOpt = codebaseIndexService.getIndex(
                    finding.getRepositoryUrl(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            String codebaseIndex = indexOpt.map(CodebaseIndexEntity::getIndexData).orElse("{}");
            
            // Build fix confirmation prompt
            String prompt = buildFixConfirmationPrompt(finding, catalog, codebaseIndex);
            Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
            
            // Use LLM for confirmation (first LLM final review)
            ChatResponse response = chatClient.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();
            
            // Parse response
            ConfirmationResult result = parseConfirmationResponse(content);
            
            // Update finding
            finding.setFixConfidence(result.confidence);
            finding.setStatus(BugFinding.BugFindingStatus.FIX_CONFIRMED);
            finding.setVerificationNotes(
                    (finding.getVerificationNotes() != null ? finding.getVerificationNotes() + "\n\n" : "") +
                    "Fix Confirmation: " + result.notes);
            
            return finding;
            
        } catch (Exception e) {
            log.error("Error confirming fix for finding {}", finding.getId(), e);
            finding.setFixConfidence(0.0);
            finding.setRequiresHumanReview(true);
            return finding;
        }
    }
    
    /**
     * Build verification prompt for second LLM.
     */
    private String buildVerificationPrompt(BugFinding finding, 
                                          com.bugbounty.cve.entity.CVECatalogEntity catalog,
                                          String codebaseIndex) {
        return """
                Verify the presence of this CVE in the commit.
                
                CVE: {cveId}
                Summary: {summary}
                Vulnerable Pattern: {vulnerablePattern}
                
                Commit Diff:
                {commitDiff}
                
                Affected Files:
                {affectedFiles}
                
                Codebase Structure:
                {codebaseIndex}
                
                Initial Analysis Notes:
                {initialNotes}
                
                Verify:
                1. Is this CVE definitely present in the commit?
                2. What is your confidence level (0.0-1.0)?
                3. What specific code makes it vulnerable?
                
                Respond with JSON:
                {{
                  "present": true/false,
                  "confidence": 0.0-1.0,
                  "vulnerableCode": "specific vulnerable code",
                  "notes": "verification notes"
                }}
                """.replace("{cveId}", finding.getCveId())
                .replace("{summary}", catalog.getSummary())
                .replace("{vulnerablePattern}", catalog.getVulnerablePattern() != null 
                        ? catalog.getVulnerablePattern() : "")
                .replace("{commitDiff}", finding.getCommitDiff() != null ? finding.getCommitDiff() : "")
                .replace("{affectedFiles}", finding.getAffectedFiles() != null 
                        ? String.join("\n", finding.getAffectedFiles()) : "")
                .replace("{codebaseIndex}", codebaseIndex)
                .replace("{initialNotes}", finding.getVerificationNotes() != null 
                        ? finding.getVerificationNotes() : "");
    }
    
    /**
     * Build fix generation prompt for second LLM.
     */
    private String buildFixGenerationPrompt(BugFinding finding,
                                           com.bugbounty.cve.entity.CVECatalogEntity catalog,
                                           String codebaseIndex) {
        return """
                Generate a code fix for this CVE vulnerability.
                
                CVE: {cveId}
                Summary: {summary}
                Fixed Pattern: {fixedPattern}
                
                Vulnerable Code:
                {vulnerableCode}
                
                Commit Diff:
                {commitDiff}
                
                Affected Files:
                {affectedFiles}
                
                Codebase Structure:
                {codebaseIndex}
                
                Generate:
                1. Complete fix code that addresses the vulnerability
                2. Ensure the fix follows the codebase patterns
                3. Include all necessary changes
                
                Respond with JSON:
                {{
                  "fixCode": "complete fix code",
                  "notes": "fix generation notes"
                }}
                """.replace("{cveId}", finding.getCveId())
                .replace("{summary}", catalog.getSummary())
                .replace("{fixedPattern}", catalog.getFixedPattern() != null 
                        ? catalog.getFixedPattern() : "")
                .replace("{vulnerableCode}", finding.getVerificationNotes() != null 
                        ? finding.getVerificationNotes() : "")
                .replace("{commitDiff}", finding.getCommitDiff() != null ? finding.getCommitDiff() : "")
                .replace("{affectedFiles}", finding.getAffectedFiles() != null 
                        ? String.join("\n", finding.getAffectedFiles()) : "")
                .replace("{codebaseIndex}", codebaseIndex);
    }
    
    /**
     * Build fix confirmation prompt for first LLM.
     */
    private String buildFixConfirmationPrompt(BugFinding finding,
                                            com.bugbounty.cve.entity.CVECatalogEntity catalog,
                                            String codebaseIndex) {
        return """
                Review and confirm this CVE fix.
                
                CVE: {cveId}
                Summary: {summary}
                Fixed Pattern: {fixedPattern}
                
                Original Vulnerable Code:
                {vulnerableCode}
                
                Recommended Fix:
                {recommendedFix}
                
                Commit Diff:
                {commitDiff}
                
                Codebase Structure:
                {codebaseIndex}
                
                Review:
                1. Does the fix correctly address the vulnerability?
                2. Is the fix code correct and complete?
                3. Does it follow codebase patterns?
                4. What is your confidence in the fix (0.0-1.0)?
                
                Respond with JSON:
                {{
                  "correct": true/false,
                  "confidence": 0.0-1.0,
                  "notes": "review notes",
                  "suggestions": "any improvements"
                }}
                """.replace("{cveId}", finding.getCveId())
                .replace("{summary}", catalog.getSummary())
                .replace("{fixedPattern}", catalog.getFixedPattern() != null 
                        ? catalog.getFixedPattern() : "")
                .replace("{vulnerableCode}", finding.getVerificationNotes() != null 
                        ? finding.getVerificationNotes() : "")
                .replace("{recommendedFix}", finding.getRecommendedFix() != null 
                        ? finding.getRecommendedFix() : "")
                .replace("{commitDiff}", finding.getCommitDiff() != null ? finding.getCommitDiff() : "")
                .replace("{codebaseIndex}", codebaseIndex);
    }
    
    /**
     * Parse verification response.
     */
    private VerificationResult parseVerificationResponse(String content) {
        try {
            String jsonContent = extractJsonFromResponse(content);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            boolean present = node.has("present") && node.get("present").asBoolean();
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.0;
            String notes = node.has("notes") ? node.get("notes").asText() : "";
            
            if (node.has("vulnerableCode")) {
                notes += "\nVulnerable Code: " + node.get("vulnerableCode").asText();
            }
            
            return new VerificationResult(present, confidence, notes);
        } catch (Exception e) {
            log.error("Failed to parse verification response: {}", content, e);
            return new VerificationResult(false, 0.0, "Failed to parse response: " + e.getMessage());
        }
    }
    
    /**
     * Parse fix generation response.
     */
    private FixResult parseFixResponse(String content) {
        try {
            String jsonContent = extractJsonFromResponse(content);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            String fixCode = node.has("fixCode") ? node.get("fixCode").asText() : "";
            String notes = node.has("notes") ? node.get("notes").asText() : "";
            
            return new FixResult(fixCode, notes);
        } catch (Exception e) {
            log.error("Failed to parse fix response: {}", content, e);
            return new FixResult("", "Failed to parse response: " + e.getMessage());
        }
    }
    
    /**
     * Parse confirmation response.
     */
    private ConfirmationResult parseConfirmationResponse(String content) {
        try {
            String jsonContent = extractJsonFromResponse(content);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            boolean correct = node.has("correct") && node.get("correct").asBoolean();
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.0;
            String notes = node.has("notes") ? node.get("notes").asText() : "";
            
            if (node.has("suggestions")) {
                notes += "\nSuggestions: " + node.get("suggestions").asText();
            }
            
            return new ConfirmationResult(correct, confidence, notes);
        } catch (Exception e) {
            log.error("Failed to parse confirmation response: {}", content, e);
            return new ConfirmationResult(false, 0.0, "Failed to parse response: " + e.getMessage());
        }
    }
    
    /**
     * Extract JSON from LLM response.
     */
    private String extractJsonFromResponse(String content) {
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
        return jsonContent.trim();
    }
    
    /**
     * Get repository language from database.
     */
    private String getRepositoryLanguage(String repositoryUrl) {
        return repositoryRepository.findByUrl(repositoryUrl)
                .map(repo -> repo.getLanguage())
                .orElse("Java"); // Default fallback
    }
    
    /**
     * Update bug finding in database.
     */
    private void updateBugFinding(BugFinding finding) {
        BugFindingEntity entity = bugFindingMapper.toEntity(finding);
        bugFindingRepository.save(entity);
    }
    
    // Result classes
    private record VerificationResult(boolean present, double confidence, String notes) {}
    private record FixResult(String fixCode, String notes) {}
    private record ConfirmationResult(boolean correct, double confidence, String notes) {}
}

