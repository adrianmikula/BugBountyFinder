package com.bugbounty.cve.service;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.cve.domain.BugFinding;
import com.bugbounty.cve.entity.BugFindingEntity;
import com.bugbounty.cve.entity.CodebaseIndexEntity;
import com.bugbounty.cve.mapper.BugFindingMapper;
import com.bugbounty.cve.repository.BugFindingRepository;
import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.entity.RepositoryEntity;
import com.bugbounty.repository.repository.RepositoryRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for analyzing GitHub issues to understand bugs and generate fixes.
 * Scans source classes/methods mentioned in the issue and analyzes root causes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueAnalysisService {
    
    private final ChatClient chatClient;
    private final CodebaseIndexService codebaseIndexService;
    private final BugFindingRepository bugFindingRepository;
    private final BugFindingMapper bugFindingMapper;
    private final RepositoryService repositoryService;
    private final RepositoryRepository repositoryRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${app.repository.clone.base-path:./repos}")
    private String basePath;
    
    // Patterns to extract file/class/method references from issue descriptions
    private static final Pattern FILE_PATTERN = Pattern.compile(
        "(?:file|class|method|function|in|at)[\\s:]+([a-zA-Z0-9_/\\\\]+\\.(?:java|ts|js|py|go|rs|rb|php|cpp|c|h))",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "(?:class|interface|type)[\\s:]+([A-Z][a-zA-Z0-9_]*)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(?:method|function)[\\s:]+([a-zA-Z0-9_]+)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Analyze a GitHub issue (bounty) to understand the bug and identify affected code.
     * 
     * @param bounty The bounty representing a GitHub issue
     * @return Flux of BugFinding objects representing analyzed issues
     */
    public Flux<BugFinding> analyzeIssue(Bounty bounty) {
        if (bounty == null || bounty.getRepositoryUrl() == null || bounty.getIssueId() == null) {
            log.warn("Invalid bounty for issue analysis: {}", bounty);
            return Flux.empty();
        }
        
        log.info("Analyzing issue #{} in repository {} for bug root cause", 
                bounty.getIssueId(), bounty.getRepositoryUrl());
        
        return Mono.fromCallable(() -> {
            // Get repository information
            Optional<RepositoryEntity> repoEntity = repositoryRepository.findByUrl(bounty.getRepositoryUrl());
            if (repoEntity.isEmpty()) {
                log.warn("Repository not found in database: {}", bounty.getRepositoryUrl());
                return null;
            }
            
            RepositoryEntity repo = repoEntity.get();
            String language = repo.getLanguage();
            if (language == null || language.isEmpty()) {
                log.warn("Repository language not set: {}", bounty.getRepositoryUrl());
                return null;
            }
            
            // Ensure repository is cloned
            Repository repository = Repository.builder()
                    .url(bounty.getRepositoryUrl())
                    .build();
            
            if (!repository.isCloned()) {
                log.info("Cloning repository for issue analysis: {}", bounty.getRepositoryUrl());
                repository = repositoryService.cloneRepository(repository, basePath);
            }
            
            // Get codebase index
            Optional<CodebaseIndexEntity> indexOpt = codebaseIndexService.getIndex(
                    bounty.getRepositoryUrl(), language);
            String codebaseIndex = indexOpt.map(CodebaseIndexEntity::getIndexData).orElse("{}");
            
            // Extract mentioned files/classes/methods from issue
            List<String> mentionedFiles = extractMentionedFiles(bounty.getDescription(), 
                    bounty.getTitle(), repository);
            
            if (mentionedFiles.isEmpty()) {
                log.info("No specific files mentioned in issue #{} - will analyze based on issue description", 
                        bounty.getIssueId());
                // Still proceed with analysis using issue description
            }
            
            // Get file contents for mentioned files
            Map<String, String> fileContents = getFileContents(repository, mentionedFiles);
            
            // Analyze the issue to understand root cause
            return analyzeIssueRootCause(bounty, language, codebaseIndex, fileContents, mentionedFiles);
            
        })
        .flatMapMany(analysisResult -> {
            if (analysisResult == null) {
                return Flux.empty();
            }
            
            // Create bug finding from analysis
            BugFinding finding = createBugFindingFromAnalysis(bounty, analysisResult);
            
            // Save to database
            BugFindingEntity entity = bugFindingMapper.toEntity(finding);
            BugFindingEntity saved = bugFindingRepository.save(entity);
            BugFinding savedFinding = bugFindingMapper.toDomain(saved);
            
            log.info("Created bug finding for issue #{} in repository {}", 
                    bounty.getIssueId(), bounty.getRepositoryUrl());
            
            return Flux.just(savedFinding);
        })
        .doOnError(error -> log.error("Error analyzing issue #{}", bounty.getIssueId(), error));
    }
    
    /**
     * Extract mentioned files, classes, and methods from issue description.
     */
    private List<String> extractMentionedFiles(String description, String title, Repository repository) {
        List<String> files = new ArrayList<>();
        String combinedText = ((title != null ? title : "") + " " + 
                              (description != null ? description : "")).toLowerCase();
        
        // Extract file paths
        Matcher fileMatcher = FILE_PATTERN.matcher(combinedText);
        while (fileMatcher.find()) {
            String filePath = fileMatcher.group(1);
            // Normalize path
            if (!filePath.startsWith("/") && !filePath.contains("\\")) {
                // Try to find file in repository
                String foundFile = findFileInRepository(repository, filePath);
                if (foundFile != null) {
                    files.add(foundFile);
                }
            } else {
                files.add(filePath);
            }
        }
        
        // Extract class names and try to find corresponding files
        Matcher classMatcher = CLASS_PATTERN.matcher(combinedText);
        while (classMatcher.find()) {
            String className = classMatcher.group(1);
            String foundFile = findClassFile(repository, className);
            if (foundFile != null && !files.contains(foundFile)) {
                files.add(foundFile);
            }
        }
        
        return files.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * Find file in repository by name.
     */
    private String findFileInRepository(Repository repository, String fileName) {
        try {
            if (!repository.isCloned()) {
                return null;
            }
            
            Path repoPath = Paths.get(repository.getLocalPath());
            return Files.walk(repoPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .map(path -> repoPath.relativize(path).toString().replace("\\", "/"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Error finding file {} in repository: {}", fileName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Find file containing a class by class name.
     */
    private String findClassFile(Repository repository, String className) {
        try {
            if (!repository.isCloned()) {
                return null;
            }
            
            Path repoPath = Paths.get(repository.getLocalPath());
            return Files.walk(repoPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            String content = Files.readString(path);
                            // Look for class declaration
                            Pattern classDecl = Pattern.compile(
                                "(?:public\\s+)?(?:class|interface|enum)\\s+" + className + "\\b",
                                Pattern.CASE_INSENSITIVE
                            );
                            return classDecl.matcher(content).find();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(path -> repoPath.relativize(path).toString().replace("\\", "/"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Error finding class {} in repository: {}", className, e.getMessage());
            return null;
        }
    }
    
    /**
     * Analyze issue to understand root cause.
     */
    private IssueAnalysisResult analyzeIssueRootCause(
            Bounty bounty,
            String language,
            String codebaseIndex,
            Map<String, String> fileContents,
            List<String> mentionedFiles) {
        
        try {
            // Build analysis prompt
            String prompt = buildRootCauseAnalysisPrompt(
                    bounty, language, codebaseIndex, fileContents, mentionedFiles);
            
            Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
            ChatResponse response = chatClient.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();
            
            // Parse response
            return parseRootCauseAnalysis(content, mentionedFiles);
            
        } catch (Exception e) {
            log.error("Error analyzing root cause for issue #{}", bounty.getIssueId(), e);
            return new IssueAnalysisResult(
                    "Error analyzing root cause: " + e.getMessage(),
                    new ArrayList<>(),
                    new HashMap<>(),
                    0.0
            );
        }
    }
    
    /**
     * Build prompt for root cause analysis.
     */
    private String buildRootCauseAnalysisPrompt(
            Bounty bounty,
            String language,
            String codebaseIndex,
            Map<String, String> fileContents,
            List<String> mentionedFiles) {
        
        StringBuilder filesContent = new StringBuilder();
        for (Map.Entry<String, String> entry : fileContents.entrySet()) {
            filesContent.append(String.format("\n=== File: %s ===\n%s\n", 
                    entry.getKey(), entry.getValue()));
        }
        
        return """
                Analyze this GitHub issue to understand the root cause of the reported bug.
                
                Issue Title: {title}
                Issue Description: {description}
                
                Programming Language: {language}
                
                Mentioned Files:
                {mentionedFiles}
                
                File Contents:
                {fileContents}
                
                Codebase Structure:
                {codebaseIndex}
                
                Analyze:
                1. What is the root cause of the bug described in this issue?
                2. Which specific classes, methods, or code sections are involved?
                3. What files need to be modified to fix this issue?
                4. What is your confidence in understanding the root cause (0.0-1.0)?
                
                Respond with JSON:
                {{
                  "rootCause": "detailed explanation of the root cause",
                  "affectedFiles": ["file1.java", "file2.java"],
                  "affectedCode": {{
                    "file1.java": "specific code sections or methods"
                  }},
                  "confidence": 0.0-1.0
                }}
                """.replace("{title}", bounty.getTitle() != null ? bounty.getTitle() : "")
                .replace("{description}", bounty.getDescription() != null ? bounty.getDescription() : "")
                .replace("{language}", language)
                .replace("{mentionedFiles}", String.join("\n", mentionedFiles))
                .replace("{fileContents}", filesContent.toString())
                .replace("{codebaseIndex}", codebaseIndex);
    }
    
    /**
     * Parse root cause analysis response.
     */
    private IssueAnalysisResult parseRootCauseAnalysis(String content, List<String> defaultFiles) {
        try {
            String jsonContent = extractJsonFromResponse(content);
            JsonNode node = objectMapper.readTree(jsonContent);
            
            String rootCause = node.has("rootCause") ? node.get("rootCause").asText() : "";
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.0;
            
            List<String> affectedFiles = new ArrayList<>();
            if (node.has("affectedFiles") && node.get("affectedFiles").isArray()) {
                for (JsonNode file : node.get("affectedFiles")) {
                    affectedFiles.add(file.asText());
                }
            } else {
                affectedFiles.addAll(defaultFiles);
            }
            
            Map<String, String> affectedCode = new HashMap<>();
            if (node.has("affectedCode") && node.get("affectedCode").isObject()) {
                node.get("affectedCode").fields().forEachRemaining(entry -> {
                    affectedCode.put(entry.getKey(), entry.getValue().asText());
                });
            }
            
            return new IssueAnalysisResult(rootCause, affectedFiles, affectedCode, confidence);
            
        } catch (Exception e) {
            log.error("Failed to parse root cause analysis response: {}", content, e);
            return new IssueAnalysisResult(
                    "Failed to parse analysis: " + e.getMessage(),
                    defaultFiles,
                    new HashMap<>(),
                    0.0
            );
        }
    }
    
    /**
     * Create BugFinding from analysis result.
     */
    private BugFinding createBugFindingFromAnalysis(Bounty bounty, IssueAnalysisResult analysis) {
        return BugFinding.builder()
                .repositoryUrl(bounty.getRepositoryUrl())
                .issueId(bounty.getIssueId()) // Store issue ID instead of commit ID
                .issueTitle(bounty.getTitle())
                .issueDescription(bounty.getDescription())
                .status(BugFinding.BugFindingStatus.DETECTED)
                .rootCauseConfidence(analysis.confidence())
                .rootCauseAnalysis(analysis.rootCause())
                .affectedFiles(analysis.affectedFiles())
                .affectedCode(analysis.affectedCode())
                .requiresHumanReview(analysis.confidence() < 0.7)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }
    
    /**
     * Get file contents for mentioned files.
     */
    private Map<String, String> getFileContents(Repository repository, List<String> filePaths) {
        Map<String, String> contents = new HashMap<>();
        
        if (!repository.isCloned()) {
            log.warn("Repository not cloned, cannot read file contents: {}", repository.getUrl());
            return contents;
        }
        
        try {
            String localPath = repository.getLocalPath();
            Path repoPath = Paths.get(localPath);
            
            for (String filePath : filePaths) {
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
            log.error("Error getting file contents for repository {}", repository.getUrl(), e);
        }
        
        return contents;
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
     * Result of issue analysis.
     */
    private record IssueAnalysisResult(
            String rootCause,
            List<String> affectedFiles,
            Map<String, String> affectedCode,
            double confidence
    ) {}
}

