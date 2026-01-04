package com.bugbounty.cve.repository;

import com.bugbounty.cve.entity.BugFindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BugFindingRepository extends JpaRepository<BugFindingEntity, UUID> {
    
    Optional<BugFindingEntity> findByRepositoryUrlAndCommitIdAndCveId(
            String repositoryUrl, String commitId, String cveId);
    
    List<BugFindingEntity> findByRepositoryUrlAndCommitId(String repositoryUrl, String commitId);
    
    List<BugFindingEntity> findByCveId(String cveId);
    
    List<BugFindingEntity> findByRequiresHumanReviewAndHumanReviewed(
            Boolean requiresHumanReview, Boolean humanReviewed);
    
    List<BugFindingEntity> findByStatus(BugFindingEntity.BugFindingStatus status);
}

