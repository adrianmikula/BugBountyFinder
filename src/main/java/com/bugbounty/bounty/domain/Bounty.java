package com.bugbounty.bounty.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bounty {
    
    @Builder.Default
    private UUID id = UUID.randomUUID();
    
    private String issueId;
    private String repositoryUrl;
    private String platform; // algora, polar, github
    private BigDecimal amount;
    private String currency;
    private String title;
    private String description;
    private BountyStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    
    private String pullRequestId;
    private String failureReason;
    
    public void markInProgress() {
        this.status = BountyStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }
    
    public void markCompleted(String pullRequestId) {
        this.status = BountyStatus.COMPLETED;
        this.pullRequestId = pullRequestId;
        this.completedAt = LocalDateTime.now();
    }
    
    public void markFailed(String reason) {
        this.status = BountyStatus.FAILED;
        this.failureReason = reason;
        this.failedAt = LocalDateTime.now();
    }
    
    public boolean isEligibleForProcessing() {
        return status == BountyStatus.OPEN;
    }
    
    public boolean meetsMinimumAmount(BigDecimal minimumAmount) {
        if (amount == null || minimumAmount == null) {
            return false;
        }
        return amount.compareTo(minimumAmount) >= 0;
    }
}

