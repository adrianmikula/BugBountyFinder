package com.bugbounty.cve.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Domain model for bug finding in a commit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugFinding {
    
    @Builder.Default
    private UUID id = UUID.randomUUID();
    
    private String repositoryUrl;
    private String commitId;
    private String cveId;
    private BugFindingStatus status;
    private Double presenceConfidence; // 0.0-1.0
    private Double fixConfidence; // 0.0-1.0
    private String commitDiff;
    private List<String> affectedFiles;
    private String recommendedFix;
    private String verificationNotes;
    private Boolean requiresHumanReview;
    private Boolean humanReviewed;
    private String humanReviewNotes;
    private String pullRequestId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum BugFindingStatus {
        DETECTED,
        VERIFIED,
        FIX_GENERATED,
        FIX_CONFIRMED,
        HUMAN_REVIEW,
        CONFIRMED,
        REJECTED,
        PR_CREATED,
        PR_MERGED
    }
    
    public boolean isHighConfidence() {
        return presenceConfidence != null && presenceConfidence >= 0.8 
            && fixConfidence != null && fixConfidence >= 0.8;
    }
    
    public boolean needsHumanReview() {
        return requiresHumanReview != null && requiresHumanReview 
            && (humanReviewed == null || !humanReviewed);
    }
}

