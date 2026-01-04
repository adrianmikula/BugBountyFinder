package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.CVE;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

/**
 * Client for interacting with the National Vulnerability Database (NVD) API.
 * NVD provides CVE data via REST API and JSON feeds.
 */
public interface NvdApiClient {
    
    /**
     * Fetch recently published CVEs from NVD.
     * 
     * @param startDate Start date for CVE search (typically last 24 hours)
     * @param endDate End date for CVE search
     * @return Flux of CVE domain objects
     */
    Flux<CVE> fetchRecentCVEs(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Fetch CVEs by CVE ID.
     * 
     * @param cveId The CVE identifier (e.g., "CVE-2024-1234")
     * @return CVE domain object or empty if not found
     */
    Flux<CVE> fetchCVEById(String cveId);
    
    /**
     * Fetch CVEs with specific severity levels.
     * 
     * @param severities List of severity levels (CRITICAL, HIGH, MEDIUM, LOW)
     * @param startDate Start date for search
     * @param endDate End date for search
     * @return Flux of CVE domain objects
     */
    Flux<CVE> fetchCVEsBySeverity(java.util.List<String> severities, LocalDateTime startDate, LocalDateTime endDate);
}

