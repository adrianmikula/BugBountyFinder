package com.bugbounty.web.controller;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.entity.BountyEntity;
import com.bugbounty.bounty.mapper.BountyMapper;
import com.bugbounty.bounty.repository.BountyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving bounties.
 */
@RestController
@RequestMapping("/api/bounties")
@RequiredArgsConstructor
@Slf4j
public class BountyController {
    
    private final BountyRepository bountyRepository;
    private final BountyMapper bountyMapper;
    
    /**
     * Get all claimed bounties (status is CLAIMED or COMPLETED).
     */
    @GetMapping("/claimed")
    public ResponseEntity<List<Bounty>> getClaimedBounties() {
        log.debug("Fetching claimed bounties");
        
        List<BountyEntity> claimedEntities = bountyRepository.findByStatus(BountyStatus.CLAIMED);
        List<BountyEntity> completedEntities = bountyRepository.findByStatus(BountyStatus.COMPLETED);
        
        claimedEntities.addAll(completedEntities);
        
        List<Bounty> bounties = claimedEntities.stream()
                .map(bountyMapper::toDomain)
                .collect(Collectors.toList());
        
        log.debug("Found {} claimed bounties", bounties.size());
        return ResponseEntity.ok(bounties);
    }
}

