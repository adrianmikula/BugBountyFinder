package com.bugbounty.web.controller;

import com.bugbounty.cve.repository.BugFindingRepository;
import com.bugbounty.cve.repository.CVERepository;
import com.bugbounty.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for retrieving system statistics.
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {
    
    private final RepositoryRepository repositoryRepository;
    private final CVERepository cveRepository;
    private final BugFindingRepository bugFindingRepository;
    
    /**
     * Get system statistics including repos watched, CVEs tracked, and commits processed today.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.debug("Fetching system statistics");
        
        // Count repositories currently watched
        long reposWatched = repositoryRepository.count();
        
        // Count CVEs currently being tracked
        long cvesTracked = cveRepository.count();
        
        // Count commits processed today (bug findings created today)
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long commitsProcessedToday = bugFindingRepository.findAll().stream()
                .filter(finding -> {
                    LocalDateTime createdAt = finding.getCreatedAt();
                    return createdAt != null 
                            && !createdAt.isBefore(startOfDay) 
                            && !createdAt.isAfter(endOfDay);
                })
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("reposWatched", reposWatched);
        stats.put("cvesTracked", cvesTracked);
        stats.put("commitsProcessedToday", commitsProcessedToday);
        
        log.debug("Statistics: repos={}, cves={}, commits today={}", 
                reposWatched, cvesTracked, commitsProcessedToday);
        
        return ResponseEntity.ok(stats);
    }
}

