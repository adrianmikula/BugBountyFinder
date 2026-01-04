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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving bug findings that need human review.
 */
@RestController
@RequestMapping("/api/bug-findings")
@RequiredArgsConstructor
@Slf4j
public class BugFindingController {
    
    private final BugFindingRepository bugFindingRepository;
    private final BugFindingMapper bugFindingMapper;
    
    /**
     * Get all bug findings that require human review (low confidence detection or low confidence fix).
     */
    @GetMapping("/needs-review")
    public ResponseEntity<List<BugFinding>> getBugFindingsNeedingReview() {
        log.debug("Fetching bug findings that need human review");
        
        // Find bug findings that require human review and haven't been reviewed yet
        List<BugFindingEntity> reviewEntities = bugFindingRepository
                .findByRequiresHumanReviewAndHumanReviewed(true, false);
        
        // Also find bug findings with low confidence (presence < 0.8 or fix < 0.8)
        List<BugFindingEntity> allEntities = bugFindingRepository.findAll();
        List<BugFindingEntity> lowConfidenceEntities = allEntities.stream()
                .filter(entity -> {
                    boolean lowPresenceConfidence = entity.getPresenceConfidence() != null 
                            && entity.getPresenceConfidence() < 0.8;
                    boolean lowFixConfidence = entity.getFixConfidence() != null 
                            && entity.getFixConfidence() < 0.8;
                    boolean verifiedButLowFix = entity.getStatus() == BugFindingEntity.BugFindingStatus.VERIFIED
                            && lowFixConfidence;
                    return (lowPresenceConfidence || verifiedButLowFix) 
                            && (entity.getHumanReviewed() == null || !entity.getHumanReviewed());
                })
                .collect(Collectors.toList());
        
        // Combine and deduplicate
        List<BugFindingEntity> entities = new ArrayList<>(reviewEntities);
        entities.addAll(lowConfidenceEntities);
        List<BugFinding> findings = entities.stream()
                .distinct()
                .map(bugFindingMapper::toDomain)
                .collect(Collectors.toList());
        
        log.debug("Found {} bug findings needing review", findings.size());
        return ResponseEntity.ok(findings);
    }
    
    /**
     * Get all bug findings with low confidence fixes (verified as present but fix confidence is low).
     */
    @GetMapping("/low-confidence-fix")
    public ResponseEntity<List<BugFinding>> getLowConfidenceFixes() {
        log.debug("Fetching bug findings with low confidence fixes");
        
        List<BugFindingEntity> entities = bugFindingRepository.findAll();
        List<BugFindingEntity> lowFixConfidence = entities.stream()
                .filter(entity -> {
                    // Verified as present but fix confidence is low
                    boolean isVerified = entity.getStatus() == BugFindingEntity.BugFindingStatus.VERIFIED
                            || entity.getStatus() == BugFindingEntity.BugFindingStatus.FIX_GENERATED;
                    boolean hasLowFixConfidence = entity.getFixConfidence() != null 
                            && entity.getFixConfidence() < 0.8;
                    return isVerified && hasLowFixConfidence;
                })
                .collect(Collectors.toList());
        
        List<BugFinding> findings = lowFixConfidence.stream()
                .map(bugFindingMapper::toDomain)
                .collect(Collectors.toList());
        
        log.debug("Found {} bug findings with low confidence fixes", findings.size());
        return ResponseEntity.ok(findings);
    }
}

