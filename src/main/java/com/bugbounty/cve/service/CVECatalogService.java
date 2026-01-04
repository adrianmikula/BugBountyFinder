package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.domain.CVECatalog;
import com.bugbounty.cve.entity.CVECatalogEntity;
import com.bugbounty.cve.mapper.CVECatalogMapper;
import com.bugbounty.cve.repository.CVECatalogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for creating language-specific CVE catalog entries using LLM analysis.
 * When a new CVE is detected, this service analyzes it for each applicable language
 * and creates catalog entries with summaries and code examples.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CVECatalogService {
    
    private final ChatClient chatClient;
    private final CVECatalogRepository catalogRepository;
    private final CVECatalogMapper catalogMapper;
    private final LanguageMappingService languageMappingService;
    private final ObjectMapper objectMapper;
    
    /**
     * Process a new CVE and create catalog entries for each applicable language.
     */
    public Flux<CVECatalog> processCVEForCatalog(CVE cve) {
        log.info("Processing CVE {} for catalog creation", cve.getCveId());
        
        // Extract affected languages
        Set<String> affectedLanguages = languageMappingService.extractLanguages(
                cve.getAffectedProducts(),
                cve.getAffectedLanguages()
        );
        
        if (affectedLanguages.isEmpty()) {
            log.warn("No languages extracted from CVE {} - skipping catalog creation", cve.getCveId());
            return Flux.empty();
        }
        
        log.info("Creating catalog entries for CVE {} in languages: {}", cve.getCveId(), affectedLanguages);
        
        return Flux.fromIterable(affectedLanguages)
                .flatMap(language -> createCatalogEntry(cve, language))
                .doOnNext(catalog -> log.info("Created catalog entry for CVE {} in language {}", 
                        catalog.getCveId(), catalog.getLanguage()))
                .doOnError(error -> log.error("Error creating catalog entries for CVE {}", cve.getCveId(), error));
    }
    
    /**
     * Create a catalog entry for a specific CVE and language.
     */
    private Mono<CVECatalog> createCatalogEntry(CVE cve, String language) {
        // Check if entry already exists
        if (catalogRepository.findByCveIdAndLanguage(cve.getCveId(), language).isPresent()) {
            log.debug("Catalog entry already exists for CVE {} in language {}", cve.getCveId(), language);
            return Mono.empty();
        }
        
        return Mono.fromCallable(() -> {
            try {
                // Build LLM prompt for CVE analysis
                String prompt = buildCatalogPrompt(cve, language);
                Prompt aiPrompt = new PromptTemplate(prompt).create(Map.of());
                
                // Call LLM to analyze CVE
                ChatResponse response = chatClient.call(aiPrompt);
                String content = response.getResult().getOutput().getContent();
                
                // Parse LLM response
                CVECatalog catalog = parseCatalogResponse(cve.getCveId(), language, content);
                
                // Save to database
                CVECatalogEntity entity = catalogMapper.toEntity(catalog);
                CVECatalogEntity saved = catalogRepository.save(entity);
                
                log.info("Created catalog entry for CVE {} in language {}", cve.getCveId(), language);
                return catalogMapper.toDomain(saved);
                
            } catch (Exception e) {
                log.error("Error creating catalog entry for CVE {} in language {}", 
                        cve.getCveId(), language, e);
                throw new RuntimeException("Failed to create catalog entry", e);
            }
        })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    /**
     * Build LLM prompt for CVE catalog analysis.
     */
    private String buildCatalogPrompt(CVE cve, String language) {
        return """
                Analyze this CVE and create a language-specific summary for {language}.
                
                CVE Details:
                - CVE ID: {cveId}
                - Description: {description}
                - Severity: {severity}
                - CVSS Score: {cvssScore}
                - Affected Products: {affectedProducts}
                
                For the {language} programming language, provide:
                1. A concise summary of the vulnerability (2-3 sentences)
                2. A code example showing the vulnerable pattern
                3. A description of the vulnerable pattern
                4. A code example showing the fixed pattern
                5. A description of the fixed pattern
                
                Respond with a JSON object:
                {{
                  "summary": "brief description of the vulnerability in {language}",
                  "codeExample": "example vulnerable code snippet",
                  "vulnerablePattern": "description of what makes code vulnerable",
                  "fixedPattern": "description of how to fix it",
                  "fixedCodeExample": "example fixed code snippet"
                }}
                """.replace("{cveId}", cve.getCveId())
                .replace("{description}", cve.getDescription() != null ? cve.getDescription() : "N/A")
                .replace("{severity}", cve.getSeverity() != null ? cve.getSeverity() : "UNKNOWN")
                .replace("{cvssScore}", cve.getCvssScore() != null ? cve.getCvssScore().toString() : "N/A")
                .replace("{affectedProducts}", String.join(", ", 
                        cve.getAffectedProducts() != null ? cve.getAffectedProducts() : List.of()))
                .replace("{language}", language);
    }
    
    /**
     * Parse LLM response into CVECatalog domain object.
     */
    private CVECatalog parseCatalogResponse(String cveId, String language, String content) {
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
            
            String summary = node.has("summary") ? node.get("summary").asText() : "";
            String codeExample = node.has("codeExample") ? node.get("codeExample").asText() : "";
            String vulnerablePattern = node.has("vulnerablePattern") ? node.get("vulnerablePattern").asText() : "";
            String fixedPattern = node.has("fixedPattern") ? node.get("fixedPattern").asText() : "";
            
            // Combine fixed pattern and code example if available
            if (node.has("fixedCodeExample")) {
                String fixedCodeExample = node.get("fixedCodeExample").asText();
                if (!fixedPattern.isEmpty()) {
                    fixedPattern += "\n\nFixed Code Example:\n" + fixedCodeExample;
                } else {
                    fixedPattern = "Fixed Code Example:\n" + fixedCodeExample;
                }
            }
            
            return CVECatalog.builder()
                    .cveId(cveId)
                    .language(language)
                    .summary(summary)
                    .codeExample(codeExample)
                    .vulnerablePattern(vulnerablePattern)
                    .fixedPattern(fixedPattern)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to parse LLM catalog response: {}", content, e);
            // Return minimal catalog entry on parse failure
            return CVECatalog.builder()
                    .cveId(cveId)
                    .language(language)
                    .summary("Failed to parse LLM response: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Get catalog entries for a specific language.
     */
    public List<CVECatalog> getCatalogEntriesByLanguage(String language) {
        return catalogRepository.findByLanguage(language).stream()
                .map(catalogMapper::toDomain)
                .toList();
    }
    
    /**
     * Get catalog entries for multiple languages.
     */
    public List<CVECatalog> getCatalogEntriesByLanguages(List<String> languages) {
        return catalogRepository.findByLanguageIn(languages).stream()
                .map(catalogMapper::toDomain)
                .toList();
    }
    
    /**
     * Get catalog entry for a specific CVE and language.
     */
    public CVECatalog getCatalogEntry(String cveId, String language) {
        return catalogRepository.findByCveIdAndLanguage(cveId, language)
                .map(catalogMapper::toDomain)
                .orElse(null);
    }
}

