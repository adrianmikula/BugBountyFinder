package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.BugFinding;
import com.bugbounty.cve.domain.CVECatalog;
import com.bugbounty.cve.entity.CodebaseIndexEntity;
import com.bugbounty.cve.mapper.BugFindingMapper;
import com.bugbounty.cve.repository.BugFindingRepository;
import com.bugbounty.cve.repository.CVECatalogRepository;
import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.service.RepositoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing commits to detect CVE vulnerabilities.
 * Uses LLM to analyze commit diffs against CVE catalog entries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommitAnalysisService {
    
    private final ChatClient chatClient;
    private final CVECatalogRepository catalogRepository;
    private final CodebaseIndexService codebaseIndexService;
    private final BugFindingRepository bugFindingRepository;
    private final BugFindingMapper bugFindingMapper;
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;
    
    @Value("${app.repository.clone.base-path:./repos}")
    private String basePath;
    
    /**
     * Analyze a commit for CVE vulnerabilities.
     * 
     * @param repositoryUrl Repository URL
     * @param commitId Commit SHA
     * @param commitDiff Git diff of the commit
     * @param affectedFiles List of affected file paths
     * @param language Programming language of the repository
     */
    public Flux<BugFinding> analyzeCommit(
            String repositoryUrl, 
            String commitId, 
            String commitDiff,
            List<String> affectedFiles,
            String language) {
        
        log.info("Analyzing commit {} in repository {} for CVE vulnerabilities", commitId, repositoryUrl);
        
        // Get relevant CVE catalog entries for this language
        List<CVECatalog> catalogEntries = catalogRepository.findByLanguage(language).stream()
                .map(catalog -> {
                    // Map entity to domain - we'll need a mapper for this
                    return CVECatalog.builder()
                            .cveId(catalog.getCveId())
                            .language(catalog.getLanguage())
                            .summary(catalog.getSummary())
                            .codeExample(catalog.getCodeExample())
                            .vulnerablePattern(catalog.getVulnerablePattern())
                            .fixedPattern(catalog.getFixedPattern())
                            .build();
                })
                .collect(Collectors.toList());
        
        if (catalogEntries.isEmpty()) {
            log.debug("No CVE catalog entries found for language {}", language);
            return Flux.empty();
        }
        
        // Get codebase index
        Optional<CodebaseIndexEntity> indexOpt = codebaseIndexService.getIndex(repositoryUrl, language);
        String codebaseIndex = indexOpt.map(CodebaseIndexEntity::getIndexData).orElse("{}");
        
        // Initial analysis: check if any CVEs are present in the commit
        return analyzeCommitForCVEs(repositoryUrl, commitId, commitDiff, affectedFiles, 
                language, catalogEntries, codebaseIndex)
                .flatMap(cveIds -> {
                    if (cveIds.isEmpty()) {
                        log.debug("No CVEs detected in commit {}", commitId);
                        return Flux.empty();
                    }
                    
                    log.info("Detected {} potential CVE(s) in commit {}: {}", 
                            cveIds.size(), commitId, cveIds);
                    
                    // Analyze each detected CVE individually
                    return Flux.fromIterable(cveIds)
                            .flatMap(cveId -> analyzeIndividualCVE(
                                    repositoryUrl, commitId, commitDiff, affectedFiles,
                                    language, cveId, catalogEntries, codebaseIndex));
                })
                .doOnError(error -> log.error("Error analyzing commit {}", commitId, error));
    }
    
    /**
     * Initial analysis: detect which CVEs might be present in the commit.
     */
    private Mono<List<String>> analyzeCommitForCVEs(
            String repositoryUrl,
            String commitId,
            String commitDiff,
            List<String> affectedFiles,
            String language,
            List<CVECatalog> catalogEntries,
            String codebaseIndex) {
        
        return Mono.fromCallable(() -> {
            try {
                // Build prompt with all relevant CVEs
                String prompt = buildInitialAnalysisPrompt(commitDiff, affectedFiles, 
                        language, catalogEntries, codebaseIndex);
                
                Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
                ChatResponse response = chatClient.call(aiPrompt);
                String content = response.getResult().getOutput().getContent();
                
                // Parse response to get list of CVE IDs
                return parseCVEListResponse(content);
                
            } catch (Exception e) {
                log.error("Error in initial CVE analysis for commit {}", commitId, e);
                return Collections.emptyList();
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    /**
     * Analyze individual CVE in detail.
     */
    private Mono<BugFinding> analyzeIndividualCVE(
            String repositoryUrl,
            String commitId,
            String commitDiff,
            List<String> affectedFiles,
            String language,
            String cveId,
            List<CVECatalog> catalogEntries,
            String codebaseIndex) {
        
        return Mono.fromCallable(() -> {
            try {
                // Find the specific CVE catalog entry
                CVECatalog catalogEntry = catalogEntries.stream()
                        .filter(c -> c.getCveId().equals(cveId))
                        .findFirst()
                        .orElse(null);
                
                if (catalogEntry == null) {
                    log.warn("CVE catalog entry not found for {}", cveId);
                    return null;
                }
                
                // Get full file contents for affected files
                Map<String, String> fileContents = getFileContents(repositoryUrl, affectedFiles);
                
                // Build focused prompt for this CVE
                String prompt = buildIndividualCVEPrompt(commitDiff, affectedFiles, language,
                        catalogEntry, codebaseIndex, fileContents);
                
                Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
                ChatResponse response = chatClient.call(aiPrompt);
                String content = response.getResult().getOutput().getContent();
                
                // Parse response and create bug finding
                BugFinding finding = parseIndividualCVEResponse(repositoryUrl, commitId, cveId,
                        commitDiff, affectedFiles, content);
                
                // Save to database
                BugFindingEntity entity = bugFindingMapper.toEntity(finding);
                BugFindingEntity saved = bugFindingRepository.save(entity);
                
                log.info("Created bug finding for CVE {} in commit {}", cveId, commitId);
                return bugFindingMapper.toDomain(saved);
                
            } catch (Exception e) {
                log.error("Error analyzing individual CVE {} in commit {}", cveId, commitId, e);
                return null;
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .filter(Objects::nonNull);
    }
    
    /**
     * Build initial analysis prompt to detect which CVEs are present.
     */
    private String buildInitialAnalysisPrompt(
            String commitDiff,
            List<String> affectedFiles,
            String language,
            List<CVECatalog> catalogEntries,
            String codebaseIndex) {
        
        StringBuilder cveList = new StringBuilder();
        for (CVECatalog entry : catalogEntries) {
            cveList.append(String.format("- %s: %s\n", entry.getCveId(), entry.getSummary()));
        }
        
        return """
                Analyze this commit diff and determine if any of the following CVEs are present.
                
                Programming Language: {language}
                
                Commit Diff:
                {commitDiff}
                
                Affected Files:
                {affectedFiles}
                
                Codebase Structure:
                {codebaseIndex}
                
                Relevant CVEs:
                {cveList}
                
                Determine which CVEs (if any) are present in this commit. Consider:
                1. Does the code change match any vulnerable patterns?
                2. Are there any security issues introduced?
                3. Does the code match the CVE descriptions?
                
                Respond with a JSON array of CVE IDs that are present:
                ["CVE-2024-1234", "CVE-2024-5678"]
                
                If no CVEs are present, return an empty array: []
                """.replace("{language}", language)
                .replace("{commitDiff}", commitDiff != null ? commitDiff : "")
                .replace("{affectedFiles}", String.join("\n", affectedFiles != null ? affectedFiles : List.of()))
                .replace("{codebaseIndex}", codebaseIndex)
                .replace("{cveList}", cveList.toString());
    }
    
    /**
     * Build focused prompt for individual CVE analysis.
     */
    private String buildIndividualCVEPrompt(
            String commitDiff,
            List<String> affectedFiles,
            String language,
            CVECatalog catalogEntry,
            String codebaseIndex,
            Map<String, String> fileContents) {
        
        StringBuilder filesContent = new StringBuilder();
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            filesContent.append(String.format("\n=== File: %s ===\n%s\n", 
                    entry.getKey(), entry.getValue()));
        }
        
        return """
                Analyze this specific CVE in detail.
                
                CVE: {cveId}
                Summary: {summary}
                Vulnerable Pattern: {vulnerablePattern}
                Fixed Pattern: {fixedPattern}
                
                Programming Language: {language}
                
                Commit Diff:
                {commitDiff}
                
                Affected Files Content:
                {fileContents}
                
                Codebase Structure:
                {codebaseIndex}
                
                Analyze:
                1. Is this CVE definitely present in the commit? (confidence 0.0-1.0)
                2. What is the specific vulnerable code?
                3. What would be the recommended fix?
                
                Respond with JSON:
                {{
                  "cveId": "{cveId}",
                  "present": true/false,
                  "confidence": 0.0-1.0,
                  "vulnerableCode": "specific code that is vulnerable",
                  "recommendedFix": "suggested fix code",
                  "notes": "additional analysis notes"
                }}
                """.replace("{cveId}", catalogEntry.getCveId())
                .replace("{summary}", catalogEntry.getSummary())
                .replace("{vulnerablePattern}", catalogEntry.getVulnerablePattern() != null 
                        ? catalogEntry.getVulnerablePattern() : "")
                .replace("{fixedPattern}", catalogEntry.getFixedPattern() != null 
                        ? catalogEntry.getFixedPattern() : "")
                .replace("{language}", language)
                .replace("{commitDiff}", commitDiff != null ? commitDiff : "")
                .replace("{fileContents}", filesContent.toString())
                .replace("{codebaseIndex}", codebaseIndex);
    }
    
    /**
     * Get file contents for affected files.
     */
    private Map<String, String> getFileContents(String repositoryUrl, List<String> affectedFiles) {
        Map<String, String> contents = new HashMap<>();
        
        try {
            // Find repository
            Repository repository = Repository.builder()
                    .url(repositoryUrl)
                    .build();
            
            if (!repository.isCloned()) {
                log.warn("Repository not cloned, cannot read file contents: {}", repositoryUrl);
                return contents;
            }
            
            String localPath = repository.getLocalPath();
            Path repoPath = Paths.get(localPath);
            
            for (String filePath : affectedFiles) {
                try {
                    Path file = repoPath.resolve(filePath);
                    if (Files.exists(file)) {
                        contents.put(filePath, Files.readString(file));
                    }
                } catch (Exception e) {
                    log.debug("Error reading file {}: {}", filePath, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error getting file contents for repository {}", repositoryUrl, e);
        }
        
        return contents;
    }
    
    /**
     * Parse initial CVE list response.
     */
    private List<String> parseCVEListResponse(String content) {
        try {
            String jsonContent = extractJsonFromResponse(content);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            if (node.isArray()) {
                List<String> cveIds = new ArrayList<>();
                for (JsonNode item : node) {
                    cveIds.add(item.asText());
                }
                return cveIds;
            }
        } catch (Exception e) {
            log.error("Failed to parse CVE list response: {}", content, e);
        }
        return Collections.emptyList();
    }
    
    /**
     * Parse individual CVE analysis response.
     */
    private BugFinding parseIndividualCVEResponse(
            String repositoryUrl,
            String commitId,
            String cveId,
            String commitDiff,
            List<String> affectedFiles,
            String content) {
        
        try {
            String jsonContent = extractJsonFromResponse(content);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            boolean present = node.has("present") && node.get("present").asBoolean();
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.0;
            String vulnerableCode = node.has("vulnerableCode") ? node.get("vulnerableCode").asText() : "";
            String recommendedFix = node.has("recommendedFix") ? node.get("recommendedFix").asText() : "";
            String notes = node.has("notes") ? node.get("notes").asText() : "";
            
            BugFinding.BugFindingStatus status = present && confidence >= 0.7
                    ? BugFinding.BugFindingStatus.DETECTED
                    : BugFinding.BugFindingStatus.DETECTED; // Will be updated by verification service
            
            return BugFinding.builder()
                    .repositoryUrl(repositoryUrl)
                    .commitId(commitId)
                    .cveId(cveId)
                    .status(status)
                    .presenceConfidence(present ? confidence : 0.0)
                    .commitDiff(commitDiff)
                    .affectedFiles(affectedFiles)
                    .recommendedFix(recommendedFix)
                    .verificationNotes(notes)
                    .requiresHumanReview(confidence < 0.7 || confidence > 0.9) // Low or very high confidence
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to parse individual CVE response: {}", content, e);
            return BugFinding.builder()
                    .repositoryUrl(repositoryUrl)
                    .commitId(commitId)
                    .cveId(cveId)
                    .status(BugFinding.BugFindingStatus.DETECTED)
                    .presenceConfidence(0.0)
                    .requiresHumanReview(true)
                    .verificationNotes("Failed to parse LLM response: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Extract JSON from LLM response (handles markdown code blocks).
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
}

