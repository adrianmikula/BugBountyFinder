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
 * Service for cross-LLM verification of issue analysis and fix generation.
 * 
 * Verification Steps:
 * 1. First cross-check: Verify we understand the root cause of the reported bug
 * 2. Second cross-check: Verify that the proposed code fix solves the reported GitHub issue
 * 
 * Legacy: This service was originally for CVE verification but has been updated
 * to work with GitHub issue analysis.
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
            
            log.info("Verifying bug finding {} for issue #{} in repository {}", 
                    findingId, finding.getIssueId(), finding.getRepositoryUrl());
            
            // Step 1: First cross-check - Verify we understand the root cause
            BugFinding rootCauseVerified = verifyRootCauseUnderstanding(finding);
            
            if (rootCauseVerified.getRootCauseConfidence() == null || 
                rootCauseVerified.getRootCauseConfidence() < 0.7) {
                log.warn("Root cause understanding verification failed or low confidence for finding {}", findingId);
                rootCauseVerified.setRequiresHumanReview(true);
                rootCauseVerified.setStatus(BugFinding.BugFindingStatus.HUMAN_REVIEW);
                updateBugFinding(rootCauseVerified);
                return rootCauseVerified;
            }
            
            // Step 2: Generate fix code
            BugFinding withFix = generateFix(rootCauseVerified);
            
            // Step 3: Second cross-check - Verify that the fix solves the GitHub issue
            BugFinding confirmed = verifyFixSolvesIssue(withFix);
            
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
     * Step 1: First cross-check - Verify we understand the root cause of the reported bug.
     */
    private BugFinding verifyRootCauseUnderstanding(BugFinding finding) {
        try {
            // Get codebase index
            Optional<CodebaseIndexEntity> indexOpt = codebaseIndexService.getIndex(
                    finding.getRepositoryUrl(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            String codebaseIndex = indexOpt.map(CodebaseIndexEntity::getIndexData).orElse("{}");
            
            // Build root cause verification prompt
            String prompt = buildRootCauseVerificationPrompt(finding, codebaseIndex);
            Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
            
            // Use LLM for verification
            ChatResponse response = chatClient.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();
            
            // Parse response
            RootCauseVerificationResult result = parseRootCauseVerificationResponse(content);
            
            // Update finding
            finding.setRootCauseConfidence(result.confidence);
            finding.setRootCauseAnalysis(
                    (finding.getRootCauseAnalysis() != null ? finding.getRootCauseAnalysis() + "\n\n" : "") +
                    "Root Cause Verification: " + result.analysis);
            finding.setStatus(BugFinding.BugFindingStatus.VERIFIED);
            finding.setVerificationNotes(
                    (finding.getVerificationNotes() != null ? finding.getVerificationNotes() + "\n\n" : "") +
                    "Root Cause Verification: " + result.notes);
            
            return finding;
            
        } catch (Exception e) {
            log.error("Error verifying root cause understanding for finding {}", finding.getId(), e);
            finding.setRootCauseConfidence(0.0);
            finding.setRequiresHumanReview(true);
            return finding;
        }
    }
    
    /**
     * Step 2: Second LLM generates fix code.
     */
    private BugFinding generateFix(BugFinding finding) {
        try {
            // Get codebase index
            Optional<CodebaseIndexEntity> indexOpt = codebaseIndexService.getIndex(
                    finding.getRepositoryUrl(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            String codebaseIndex = indexOpt.map(CodebaseIndexEntity::getIndexData).orElse("{}");
            
            // Build fix generation prompt
            String prompt = buildFixGenerationPrompt(finding, codebaseIndex);
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
     * Step 3: Second cross-check - Verify that the proposed code fix solves the reported GitHub issue.
     */
    private BugFinding verifyFixSolvesIssue(BugFinding finding) {
        try {
            // Get codebase index
            Optional<CodebaseIndexEntity> indexOpt = codebaseIndexService.getIndex(
                    finding.getRepositoryUrl(), 
                    getRepositoryLanguage(finding.getRepositoryUrl()));
            String codebaseIndex = indexOpt.map(CodebaseIndexEntity::getIndexData).orElse("{}");
            
            // Build fix verification prompt (verify it solves the GitHub issue)
            String prompt = buildFixVerificationPrompt(finding, codebaseIndex);
            Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
            
            // Use LLM for verification
            ChatResponse response = chatClient.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();
            
            // Parse response
            FixVerificationResult result = parseFixVerificationResponse(content);
            
            // Update finding
            finding.setFixConfidence(result.confidence);
            finding.setStatus(BugFinding.BugFindingStatus.FIX_CONFIRMED);
            finding.setVerificationNotes(
                    (finding.getVerificationNotes() != null ? finding.getVerificationNotes() + "\n\n" : "") +
                    "Fix Verification (solves issue): " + result.notes);
            
            return finding;
            
        } catch (Exception e) {
            log.error("Error verifying fix solves issue for finding {}", finding.getId(), e);
            finding.setFixConfidence(0.0);
            finding.setRequiresHumanReview(true);
            return finding;
        }
    }
    
    /**
     * Build root cause verification prompt - First cross-check.
     */
    private String buildRootCauseVerificationPrompt(BugFinding finding, String codebaseIndex) {
        StringBuilder affectedCodeStr = new StringBuilder();
        if (finding.getAffectedCode() != null) {
            finding.getAffectedCode().forEach((file, code) -> {
                affectedCodeStr.append(String.format("\n=== %s ===\n%s\n", file, code));
            });
        }
        
        return """
                Verify that we understand the root cause of the reported bug in this GitHub issue.
                
                Issue Title: {issueTitle}
                Issue Description: {issueDescription}
                
                Initial Root Cause Analysis:
                {rootCauseAnalysis}
                
                Affected Files:
                {affectedFiles}
                
                Affected Code Sections:
                {affectedCode}
                
                Codebase Structure:
                {codebaseIndex}
                
                Verify:
                1. Do we correctly understand the root cause of the bug described in the issue?
                2. Have we identified the correct files and code sections?
                3. Is our analysis accurate and complete?
                4. What is your confidence level (0.0-1.0) that we understand the root cause?
                
                Respond with JSON:
                {{
                  "understood": true/false,
                  "confidence": 0.0-1.0,
                  "analysis": "refined or confirmed root cause analysis",
                  "notes": "verification notes"
                }}
                """.replace("{issueTitle}", finding.getIssueTitle() != null ? finding.getIssueTitle() : "")
                .replace("{issueDescription}", finding.getIssueDescription() != null ? finding.getIssueDescription() : "")
                .replace("{rootCauseAnalysis}", finding.getRootCauseAnalysis() != null ? finding.getRootCauseAnalysis() : "")
                .replace("{affectedFiles}", finding.getAffectedFiles() != null 
                        ? String.join("\n", finding.getAffectedFiles()) : "")
                .replace("{affectedCode}", affectedCodeStr.toString())
                .replace("{codebaseIndex}", codebaseIndex);
    }
    
    /**
     * Build fix generation prompt.
     */
    private String buildFixGenerationPrompt(BugFinding finding, String codebaseIndex) {
        StringBuilder affectedCodeStr = new StringBuilder();
        if (finding.getAffectedCode() != null) {
            finding.getAffectedCode().forEach((file, code) -> {
                affectedCodeStr.append(String.format("\n=== %s ===\n%s\n", file, code));
            });
        }
        
        return """
                Generate a code fix for this GitHub issue.
                
                Issue Title: {issueTitle}
                Issue Description: {issueDescription}
                
                Root Cause Analysis:
                {rootCauseAnalysis}
                
                Affected Files:
                {affectedFiles}
                
                Affected Code Sections:
                {affectedCode}
                
                Codebase Structure:
                {codebaseIndex}
                
                Generate:
                1. Complete fix code that addresses the bug described in the issue
                2. Ensure the fix follows the codebase patterns and style
                3. Include all necessary changes to solve the reported problem
                4. Make sure the fix addresses the root cause we identified
                
                Respond with JSON:
                {{
                  "fixCode": "complete fix code",
                  "notes": "fix generation notes"
                }}
                """.replace("{issueTitle}", finding.getIssueTitle() != null ? finding.getIssueTitle() : "")
                .replace("{issueDescription}", finding.getIssueDescription() != null ? finding.getIssueDescription() : "")
                .replace("{rootCauseAnalysis}", finding.getRootCauseAnalysis() != null ? finding.getRootCauseAnalysis() : "")
                .replace("{affectedFiles}", finding.getAffectedFiles() != null 
                        ? String.join("\n", finding.getAffectedFiles()) : "")
                .replace("{affectedCode}", affectedCodeStr.toString())
                .replace("{codebaseIndex}", codebaseIndex);
    }
    
    /**
     * Build fix verification prompt - Second cross-check: Verify fix solves the GitHub issue.
     */
    private String buildFixVerificationPrompt(BugFinding finding, String codebaseIndex) {
        StringBuilder affectedCodeStr = new StringBuilder();
        if (finding.getAffectedCode() != null) {
            finding.getAffectedCode().forEach((file, code) -> {
                affectedCodeStr.append(String.format("\n=== %s ===\n%s\n", file, code));
            });
        }
        
        return """
                Verify that the proposed code fix solves the reported GitHub issue.
                
                Issue Title: {issueTitle}
                Issue Description: {issueDescription}
                
                Root Cause Analysis:
                {rootCauseAnalysis}
                
                Original Affected Code:
                {affectedCode}
                
                Recommended Fix:
                {recommendedFix}
                
                Affected Files:
                {affectedFiles}
                
                Codebase Structure:
                {codebaseIndex}
                
                Verify:
                1. Does the fix solve the specific problem described in the GitHub issue?
                2. Does it address the root cause we identified?
                3. Is the fix code correct, complete, and follows codebase patterns?
                4. Will this fix resolve the issue for the user who reported it?
                5. What is your confidence (0.0-1.0) that this fix solves the issue?
                
                Respond with JSON:
                {{
                  "solvesIssue": true/false,
                  "confidence": 0.0-1.0,
                  "notes": "verification notes",
                  "suggestions": "any improvements or concerns"
                }}
                """.replace("{issueTitle}", finding.getIssueTitle() != null ? finding.getIssueTitle() : "")
                .replace("{issueDescription}", finding.getIssueDescription() != null ? finding.getIssueDescription() : "")
                .replace("{rootCauseAnalysis}", finding.getRootCauseAnalysis() != null ? finding.getRootCauseAnalysis() : "")
                .replace("{affectedCode}", affectedCodeStr.toString())
                .replace("{recommendedFix}", finding.getRecommendedFix() != null 
                        ? finding.getRecommendedFix() : "")
                .replace("{affectedFiles}", finding.getAffectedFiles() != null 
                        ? String.join("\n", finding.getAffectedFiles()) : "")
                .replace("{codebaseIndex}", codebaseIndex);
    }
    
    /**
     * Parse root cause verification response.
     */
    private RootCauseVerificationResult parseRootCauseVerificationResponse(String content) {
        try {
            String jsonContent = extractJsonFromResponse(content);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            boolean understood = node.has("understood") && node.get("understood").asBoolean();
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.0;
            String analysis = node.has("analysis") ? node.get("analysis").asText() : "";
            String notes = node.has("notes") ? node.get("notes").asText() : "";
            
            return new RootCauseVerificationResult(understood, confidence, analysis, notes);
        } catch (Exception e) {
            log.error("Failed to parse root cause verification response: {}", content, e);
            return new RootCauseVerificationResult(false, 0.0, "", "Failed to parse response: " + e.getMessage());
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
     * Parse fix verification response.
     */
    private FixVerificationResult parseFixVerificationResponse(String content) {
        try {
            String jsonContent = extractJsonFromResponse(content);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            boolean solvesIssue = node.has("solvesIssue") && node.get("solvesIssue").asBoolean();
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.0;
            String notes = node.has("notes") ? node.get("notes").asText() : "";
            
            if (node.has("suggestions")) {
                notes += "\nSuggestions: " + node.get("suggestions").asText();
            }
            
            return new FixVerificationResult(solvesIssue, confidence, notes);
        } catch (Exception e) {
            log.error("Failed to parse fix verification response: {}", content, e);
            return new FixVerificationResult(false, 0.0, "Failed to parse response: " + e.getMessage());
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
    private record RootCauseVerificationResult(boolean understood, double confidence, String analysis, String notes) {}
    private record FixResult(String fixCode, String notes) {}
    private record FixVerificationResult(boolean solvesIssue, double confidence, String notes) {}
}

