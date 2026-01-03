package com.bugbounty.bounty.entity;

import com.bugbounty.bounty.domain.BountyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bounties", indexes = {
    @Index(name = "idx_issue_platform", columnList = "issueId,platform"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_platform_status", columnList = "platform,status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BountyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String issueId;

    @Column(nullable = false)
    private String repositoryUrl;

    @Column(nullable = false)
    private String platform;

    private BigDecimal amount;
    private String currency;
    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BountyStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;

    private String pullRequestId;
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BountyStatus.OPEN;
        }
    }
}

