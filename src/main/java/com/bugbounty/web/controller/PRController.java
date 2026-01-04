package com.bugbounty.web.controller;

import com.bugbounty.cve.domain.BugFinding;
import com.bugbounty.cve.entity.BugFindingEntity;
import com.bugbounty.cve.mapper.BugFindingMapper;
import com.bugbounty.cve.repository.BugFindingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving historical PRs.
 */
@RestController
@RequestMapping("/api/prs")
@RequiredArgsConstructor
@Slf4j
public class PRController {
    
    private final BugFindingRepository bugFindingRepository;
    private final BugFindingMapper bugFindingMapper;
    
    /**
     * Get all historical PRs (bug findings that have a pullRequestId).
     */
    @GetMapping("/history")
    public ResponseEntity<List<BugFinding>> getHistoricalPRs() {
        log.debug("Fetching historical PRs");
        
        List<BugFindingEntity> entities = bugFindingRepository.findAll();
        List<BugFindingEntity> prEntities = entities.stream()
                .filter(entity -> entity.getPullRequestId() != null 
                        && !entity.getPullRequestId().isEmpty())
                .collect(Collectors.toList());
        
        List<BugFinding> findings = prEntities.stream()
                .map(bugFindingMapper::toDomain)
                .collect(Collectors.toList());
        
        log.debug("Found {} historical PRs", findings.size());
        return ResponseEntity.ok(findings);
    }
}

