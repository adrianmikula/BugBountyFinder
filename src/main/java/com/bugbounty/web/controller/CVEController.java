package com.bugbounty.web.controller;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.entity.CVEEntity;
import com.bugbounty.cve.mapper.CVEMapper;
import com.bugbounty.cve.repository.CVERepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving CVEs from the database.
 */
@RestController
@RequestMapping("/api/cves")
@RequiredArgsConstructor
@Slf4j
public class CVEController {
    
    private final CVERepository cveRepository;
    private final CVEMapper cveMapper;
    
    /**
     * Get all CVEs from the database.
     */
    @GetMapping
    public ResponseEntity<List<CVE>> getAllCVEs() {
        log.debug("Fetching all CVEs");
        List<CVE> cves = cveRepository.findAll().stream()
                .map(cveMapper::toDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(cves);
    }
    
    /**
     * Get CVEs by language (based on affected languages).
     * 
     * @param language The programming language to filter by
     * @return List of CVEs that affect the specified language
     */
    @GetMapping("/by-language")
    public ResponseEntity<List<CVE>> getCVEsByLanguage(@RequestParam String language) {
        log.debug("Fetching CVEs for language: {}", language);
        
        // Get all CVEs and filter by affected languages
        List<CVE> cves = cveRepository.findAll().stream()
                .map(cveMapper::toDomain)
                .filter(cve -> {
                    if (cve.getAffectedLanguages() == null) {
                        return false;
                    }
                    return cve.getAffectedLanguages().stream()
                            .anyMatch(lang -> lang.equalsIgnoreCase(language));
                })
                .collect(Collectors.toList());
        
        log.debug("Found {} CVEs for language {}", cves.size(), language);
        return ResponseEntity.ok(cves);
    }
}

