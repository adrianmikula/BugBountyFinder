package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.entity.CVEEntity;
import com.bugbounty.cve.mapper.CVEMapper;
import com.bugbounty.cve.repository.CVERepository;
import com.bugbounty.repository.entity.RepositoryEntity;
import com.bugbounty.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Service for monitoring new CVEs and triggering repository scans.
 * Supports both scheduled polling and webhook-based notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CVEMonitoringService {
    
    private final NvdApiClient nvdApiClient;
    private final CVERepository cveRepository;
    private final CVEMapper cveMapper;
    private final LanguageMappingService languageMappingService;
    private final RepositoryScanningService repositoryScanningService;
    private final RepositoryRepository repositoryRepository;
    private final CVECatalogService cveCatalogService;
    
    /**
     * Poll NVD API for new CVEs and process them.
     * Runs every hour by default (configurable via application.yml).
     */
    @Scheduled(fixedDelayString = "${app.cve.monitoring.poll-interval-ms:3600000}")
    public void pollForNewCVEs() {
        log.info("Starting scheduled CVE polling");
        
        try {
            // Fetch CVEs from last 7 days (1 week)
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.minusDays(7);
            
            // Only fetch CRITICAL and HIGH severity CVEs by default
            List<String> severities = List.of("CRITICAL", "HIGH");
            
            nvdApiClient.fetchCVEsBySeverity(severities, startDate, endDate)
                    .filter(cve -> !cveRepository.existsByCveId(cve.getCveId()))
                    .flatMap(this::processNewCVE)
                    .doOnComplete(() -> log.info("CVE polling completed"))
                    .doOnError(error -> log.error("Error during CVE polling", error))
                    .subscribe();
        } catch (Exception e) {
            log.error("Error in scheduled CVE polling", e);
        }
    }
    
    /**
     * Process a new CVE: save it and trigger repository scans.
     */
    public Flux<Void> processNewCVE(CVE cve) {
        log.info("Processing new CVE: {} (Severity: {})", cve.getCveId(), cve.getSeverity());
        
        // Check if CVE already exists
        if (cveRepository.existsByCveId(cve.getCveId())) {
            log.debug("CVE {} already exists, skipping", cve.getCveId());
            return Flux.empty();
        }
        
        try {
            // Save CVE to database
            CVEEntity entity = cveMapper.toEntity(cve);
            cveRepository.save(entity);
            log.info("Saved CVE {} to database", cve.getCveId());
            
            // Extract affected languages
            Set<String> affectedLanguages = languageMappingService.extractLanguages(
                    cve.getAffectedProducts(),
                    cve.getAffectedLanguages()
            );
            
            if (affectedLanguages.isEmpty()) {
                log.warn("No languages extracted from CVE {} - skipping repository scan", cve.getCveId());
                return Flux.empty();
            }
            
            log.info("CVE {} affects languages: {}", cve.getCveId(), affectedLanguages);
            
            // Create CVE catalog entries for each language
            cveCatalogService.processCVEForCatalog(cve)
                    .doOnNext(catalog -> log.info("Created catalog entry for CVE {} in language {}", 
                            catalog.getCveId(), catalog.getLanguage()))
                    .doOnError(error -> log.error("Error creating catalog entries for CVE {}", cve.getCveId(), error))
                    .subscribe();
            
            // Find repositories using affected languages
            List<RepositoryEntity> repositories = repositoryRepository.findAll();
            
            return Flux.fromIterable(repositories)
                    .filter(repo -> {
                        String repoLanguage = repo.getLanguage();
                        if (repoLanguage == null || repoLanguage.isEmpty()) {
                            return false;
                        }
                        return languageMappingService.matchesLanguage(repoLanguage, affectedLanguages);
                    })
                    .flatMap(repo -> {
                        log.info("Triggering scan for repository {} (language: {}) due to CVE {}",
                                repo.getUrl(), repo.getLanguage(), cve.getCveId());
                        return repositoryScanningService.scanRepositoryForCVE(
                                repo.getUrl(),
                                cve.getCveId()
                        );
                    })
                    .doOnError(error -> log.error("Error scanning repositories for CVE {}", cve.getCveId(), error));
        } catch (Exception e) {
            log.error("Error processing CVE {}", cve.getCveId(), e);
            return Flux.error(e);
        }
    }
    
    /**
     * Handle CVE notification from webhook.
     */
    public void handleCVEWebhook(CVE cve) {
        log.info("Received CVE webhook notification: {}", cve.getCveId());
        
        // Check if CVE already exists
        if (cveRepository.existsByCveId(cve.getCveId())) {
            log.debug("CVE {} already exists, skipping", cve.getCveId());
            return;
        }
        
        // Process the CVE
        processNewCVE(cve)
                .doOnComplete(() -> log.info("CVE webhook processed successfully: {}", cve.getCveId()))
                .doOnError(error -> log.error("Error processing CVE webhook: {}", cve.getCveId(), error))
                .subscribe();
    }
}

