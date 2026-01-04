package com.bugbounty.cve.service;

import com.bugbounty.cve.entity.CodebaseIndexEntity;
import com.bugbounty.cve.repository.CodebaseIndexRepository;
import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.service.RepositoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for creating and managing codebase indexes.
 * Indexes repository structure (packages, classes, methods) for LLM context.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodebaseIndexService {
    
    private final CodebaseIndexRepository indexRepository;
    private final RepositoryService repositoryService;
    private final ObjectMapper objectMapper;
    
    @Value("${app.repository.clone.base-path:./repos}")
    private String basePath;
    
    /**
     * Create or update codebase index for a repository.
     */
    public CodebaseIndexEntity indexRepository(Repository repository, String language) {
        log.info("Indexing repository {} for language {}", repository.getUrl(), language);
        
        if (!repository.isCloned()) {
            log.warn("Repository not cloned, cannot create index: {}", repository.getUrl());
            return null;
        }
        
        try {
            String localPath = repository.getLocalPath();
            Path repoPath = Paths.get(localPath);
            
            // Build index based on language
            Map<String, Object> indexData = switch (language.toLowerCase()) {
                case "java" -> indexJavaCodebase(repoPath);
                case "python" -> indexPythonCodebase(repoPath);
                case "javascript", "typescript" -> indexJavaScriptCodebase(repoPath);
                default -> indexGenericCodebase(repoPath);
            };
            
            // Convert to JSON
            String indexJson = objectMapper.writeValueAsString(indexData);
            
            // Save or update index
            Optional<CodebaseIndexEntity> existing = indexRepository.findByRepositoryUrlAndLanguage(
                    repository.getUrl(), language);
            
            CodebaseIndexEntity index;
            if (existing.isPresent()) {
                index = existing.get();
                index.setIndexData(indexJson);
                index.setIndexVersion(index.getIndexVersion() + 1);
                index.setUpdatedAt(LocalDateTime.now());
            } else {
                index = CodebaseIndexEntity.builder()
                        .repositoryUrl(repository.getUrl())
                        .language(language)
                        .indexData(indexJson)
                        .indexVersion(1)
                        .createdAt(LocalDateTime.now())
                        .build();
            }
            
            CodebaseIndexEntity saved = indexRepository.save(index);
            log.info("Indexed repository {} for language {} (version {})", 
                    repository.getUrl(), language, saved.getIndexVersion());
            
            return saved;
            
        } catch (Exception e) {
            log.error("Error indexing repository {} for language {}", repository.getUrl(), language, e);
            throw new RuntimeException("Failed to index repository", e);
        }
    }
    
    /**
     * Get codebase index for a repository and language.
     */
    public Optional<CodebaseIndexEntity> getIndex(String repositoryUrl, String language) {
        return indexRepository.findByRepositoryUrlAndLanguage(repositoryUrl, language);
    }
    
    /**
     * Index Java codebase structure.
     */
    private Map<String, Object> indexJavaCodebase(Path repoPath) throws Exception {
        Map<String, Object> index = new HashMap<>();
        List<Map<String, Object>> packages = new ArrayList<>();
        
        // Find all Java source files
        Files.walk(repoPath)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().contains("/test/"))
                .forEach(javaFile -> {
                    try {
                        String relativePath = repoPath.relativize(javaFile).toString();
                        String packageName = extractJavaPackage(javaFile);
                        String className = javaFile.getFileName().toString().replace(".java", "");
                        
                        Map<String, Object> pkg = new HashMap<>();
                        pkg.put("package", packageName);
                        pkg.put("class", className);
                        pkg.put("path", relativePath);
                        pkg.put("methods", extractJavaMethods(javaFile));
                        packages.add(pkg);
                    } catch (Exception e) {
                        log.debug("Error indexing Java file: {}", javaFile, e);
                    }
                });
        
        index.put("language", "Java");
        index.put("packages", packages);
        index.put("totalClasses", packages.size());
        
        return index;
    }
    
    /**
     * Index Python codebase structure.
     */
    private Map<String, Object> indexPythonCodebase(Path repoPath) throws Exception {
        Map<String, Object> index = new HashMap<>();
        List<Map<String, Object>> modules = new ArrayList<>();
        
        Files.walk(repoPath)
                .filter(path -> path.toString().endsWith(".py"))
                .filter(path -> !path.toString().contains("/test/"))
                .forEach(pyFile -> {
                    try {
                        String relativePath = repoPath.relativize(pyFile).toString();
                        String moduleName = pyFile.getFileName().toString().replace(".py", "");
                        
                        Map<String, Object> module = new HashMap<>();
                        module.put("module", moduleName);
                        module.put("path", relativePath);
                        module.put("functions", extractPythonFunctions(pyFile));
                        modules.add(module);
                    } catch (Exception e) {
                        log.debug("Error indexing Python file: {}", pyFile, e);
                    }
                });
        
        index.put("language", "Python");
        index.put("modules", modules);
        index.put("totalModules", modules.size());
        
        return index;
    }
    
    /**
     * Index JavaScript/TypeScript codebase structure.
     */
    private Map<String, Object> indexJavaScriptCodebase(Path repoPath) throws Exception {
        Map<String, Object> index = new HashMap<>();
        List<Map<String, Object>> modules = new ArrayList<>();
        
        Files.walk(repoPath)
                .filter(path -> {
                    String fileName = path.toString();
                    return fileName.endsWith(".js") || fileName.endsWith(".ts") || fileName.endsWith(".jsx") || fileName.endsWith(".tsx");
                })
                .filter(path -> !path.toString().contains("/test/"))
                .forEach(jsFile -> {
                    try {
                        String relativePath = repoPath.relativize(jsFile).toString();
                        String moduleName = jsFile.getFileName().toString();
                        
                        Map<String, Object> module = new HashMap<>();
                        module.put("module", moduleName);
                        module.put("path", relativePath);
                        module.put("exports", extractJavaScriptExports(jsFile));
                        modules.add(module);
                    } catch (Exception e) {
                        log.debug("Error indexing JavaScript file: {}", jsFile, e);
                    }
                });
        
        index.put("language", "JavaScript");
        index.put("modules", modules);
        index.put("totalModules", modules.size());
        
        return index;
    }
    
    /**
     * Generic codebase indexing (file structure only).
     */
    private Map<String, Object> indexGenericCodebase(Path repoPath) throws Exception {
        Map<String, Object> index = new HashMap<>();
        List<String> sourceFiles = new ArrayList<>();
        
        Files.walk(repoPath)
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.toString();
                    return !fileName.contains("/test/") 
                        && !fileName.contains("/.git/")
                        && !fileName.contains("/node_modules/")
                        && !fileName.contains("/target/")
                        && !fileName.contains("/build/");
                })
                .forEach(file -> {
                    try {
                        sourceFiles.add(repoPath.relativize(file).toString());
                    } catch (Exception e) {
                        log.debug("Error indexing file: {}", file, e);
                    }
                });
        
        index.put("sourceFiles", sourceFiles);
        index.put("totalFiles", sourceFiles.size());
        
        return index;
    }
    
    // Helper methods for extracting code structure
    
    private String extractJavaPackage(Path javaFile) throws Exception {
        List<String> lines = Files.readAllLines(javaFile);
        for (String line : lines) {
            if (line.trim().startsWith("package ")) {
                return line.trim().replace("package ", "").replace(";", "").trim();
            }
        }
        return "default";
    }
    
    private List<String> extractJavaMethods(Path javaFile) throws Exception {
        List<String> methods = new ArrayList<>();
        List<String> lines = Files.readAllLines(javaFile);
        for (String line : lines) {
            if (line.trim().matches(".*(public|private|protected)\\s+.*\\(.*\\).*\\{")) {
                methods.add(line.trim());
            }
        }
        return methods;
    }
    
    private List<String> extractPythonFunctions(Path pyFile) throws Exception {
        List<String> functions = new ArrayList<>();
        List<String> lines = Files.readAllLines(pyFile);
        for (String line : lines) {
            if (line.trim().startsWith("def ") || line.trim().startsWith("async def ")) {
                functions.add(line.trim());
            }
        }
        return functions;
    }
    
    private List<String> extractJavaScriptExports(Path jsFile) throws Exception {
        List<String> exports = new ArrayList<>();
        List<String> lines = Files.readAllLines(jsFile);
        for (String line : lines) {
            if (line.trim().contains("export ") || line.trim().contains("module.exports")) {
                exports.add(line.trim());
            }
        }
        return exports;
    }
}

