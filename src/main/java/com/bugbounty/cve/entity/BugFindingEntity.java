package com.bugbounty.cve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking potential CVE bugs found in commits.
 */
@Entity
@Table(name = "bug_findings", indexes = {
    @Index(name = "idx_bug_findings_repo_commit", columnList = "repositoryUrl,commitId"),
    @Index(name = "idx_bug_findings_repo_issue", columnList = "repositoryUrl,issueId"),
    @Index(name = "idx_bug_findings_cve", columnList = "cveId"),
    @Index(name = "idx_bug_findings_status", columnList = "status"),
    @Index(name = "idx_bug_findings_human_review", columnList = "requiresHumanReview,humanReviewed")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugFindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String repositoryUrl;

    // Issue-related fields (new)
    @Column(length = 50)
    private String issueId; // GitHub issue number
    
    @Column(length = 500)
    private String issueTitle;
    
    @Column(columnDefinition = "TEXT")
    private String issueDescription;
    
    // Root cause analysis fields (new)
    @Column(columnDefinition = "TEXT")
    private String rootCauseAnalysis; // Detailed explanation of root cause
    
    @Column(name = "root_cause_confidence")
    private Double rootCauseConfidence; // 0.0-1.0
    
    @Column(columnDefinition = "TEXT")
    private String affectedCode; // JSON map of file -> code sections/methods

    // Legacy CVE fields (now nullable for issue-based analysis)
    @Column(length = 255)
    private String commitId;

    @Column(length = 50)
    private String cveId;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BugFindingStatus status = BugFindingStatus.DETECTED;

    @Column(name = "presence_confidence")
    private Double presenceConfidence; // 0.0-1.0, confidence that CVE is present

    @Column(name = "fix_confidence")
    private Double fixConfidence; // 0.0-1.0, confidence in the fix correctness

    @Column(columnDefinition = "TEXT")
    private String commitDiff; // Git diff of the commit

    @Column(columnDefinition = "TEXT")
    private String affectedFiles; // JSON array of affected file paths

    @Column(columnDefinition = "TEXT")
    private String recommendedFix; // LLM-generated fix code

    @Column(columnDefinition = "TEXT")
    private String verificationNotes; // Notes from LLM verification

    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresHumanReview = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean humanReviewed = false;

    @Column(columnDefinition = "TEXT")
    private String humanReviewNotes;

    @Column(length = 255)
    private String pullRequestId; // GitHub PR ID if created

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BugFindingStatus {
        DETECTED,           // Initial detection
        VERIFIED,           // Verified by second LLM
        FIX_GENERATED,      // Fix code generated
        FIX_CONFIRMED,      // Fix confirmed by first LLM
        HUMAN_REVIEW,       // Requires human review
        CONFIRMED,          // Human confirmed
        REJECTED,           // Human rejected
        PR_CREATED,         // PR created
        PR_MERGED           // PR merged
    }
}

