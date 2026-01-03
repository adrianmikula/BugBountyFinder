package com.bugbounty.bounty.repository;

import com.bugbounty.bounty.entity.BountyEntity;
import com.bugbounty.bounty.domain.BountyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BountyRepository extends JpaRepository<BountyEntity, UUID> {
    
    boolean existsByIssueIdAndPlatform(String issueId, String platform);
    
    List<BountyEntity> findByStatus(BountyStatus status);
    
    List<BountyEntity> findByPlatformAndStatus(String platform, BountyStatus status);
}

