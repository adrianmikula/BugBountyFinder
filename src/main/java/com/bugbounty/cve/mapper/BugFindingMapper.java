package com.bugbounty.cve.mapper;

import com.bugbounty.cve.domain.BugFinding;
import com.bugbounty.cve.entity.BugFindingEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BugFindingMapper {
    
    private final ObjectMapper objectMapper;
    
    public BugFindingEntity toEntity(BugFinding finding) {
        if (finding == null) {
            return null;
        }
        
        try {
            return BugFindingEntity.builder()
                    .id(finding.getId())
                    .repositoryUrl(finding.getRepositoryUrl())
                    .issueId(finding.getIssueId())
                    .issueTitle(finding.getIssueTitle())
                    .issueDescription(finding.getIssueDescription())
                    .rootCauseAnalysis(finding.getRootCauseAnalysis())
                    .rootCauseConfidence(finding.getRootCauseConfidence())
                    .affectedCode(finding.getAffectedCode() != null 
                            ? objectMapper.writeValueAsString(finding.getAffectedCode()) 
                            : null)
                    .commitId(finding.getCommitId())
                    .cveId(finding.getCveId())
                    .status(mapStatus(finding.getStatus()))
                    .presenceConfidence(finding.getPresenceConfidence())
                    .fixConfidence(finding.getFixConfidence())
                    .commitDiff(finding.getCommitDiff())
                    .affectedFiles(finding.getAffectedFiles() != null 
                            ? objectMapper.writeValueAsString(finding.getAffectedFiles()) 
                            : null)
                    .recommendedFix(finding.getRecommendedFix())
                    .verificationNotes(finding.getVerificationNotes())
                    .requiresHumanReview(finding.getRequiresHumanReview() != null 
                            ? finding.getRequiresHumanReview() 
                            : false)
                    .humanReviewed(finding.getHumanReviewed() != null 
                            ? finding.getHumanReviewed() 
                            : false)
                    .humanReviewNotes(finding.getHumanReviewNotes())
                    .pullRequestId(finding.getPullRequestId())
                    .createdAt(finding.getCreatedAt())
                    .updatedAt(finding.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map BugFinding to entity", e);
        }
    }
    
    public BugFinding toDomain(BugFindingEntity entity) {
        if (entity == null) {
            return null;
        }
        
        try {
            List<String> affectedFiles = null;
            if (entity.getAffectedFiles() != null && !entity.getAffectedFiles().isEmpty()) {
                affectedFiles = objectMapper.readValue(
                        entity.getAffectedFiles(), 
                        new TypeReference<List<String>>() {}
                );
            }
            
            Map<String, String> affectedCode = null;
            if (entity.getAffectedCode() != null && !entity.getAffectedCode().isEmpty()) {
                affectedCode = objectMapper.readValue(
                        entity.getAffectedCode(),
                        new TypeReference<Map<String, String>>() {}
                );
            }
            
            return BugFinding.builder()
                    .id(entity.getId())
                    .repositoryUrl(entity.getRepositoryUrl())
                    .issueId(entity.getIssueId())
                    .issueTitle(entity.getIssueTitle())
                    .issueDescription(entity.getIssueDescription())
                    .rootCauseAnalysis(entity.getRootCauseAnalysis())
                    .rootCauseConfidence(entity.getRootCauseConfidence())
                    .affectedCode(affectedCode)
                    .commitId(entity.getCommitId())
                    .cveId(entity.getCveId())
                    .status(mapStatus(entity.getStatus()))
                    .presenceConfidence(entity.getPresenceConfidence())
                    .fixConfidence(entity.getFixConfidence())
                    .commitDiff(entity.getCommitDiff())
                    .affectedFiles(affectedFiles)
                    .recommendedFix(entity.getRecommendedFix())
                    .verificationNotes(entity.getVerificationNotes())
                    .requiresHumanReview(entity.getRequiresHumanReview())
                    .humanReviewed(entity.getHumanReviewed())
                    .humanReviewNotes(entity.getHumanReviewNotes())
                    .pullRequestId(entity.getPullRequestId())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map BugFindingEntity to domain", e);
        }
    }
    
    private BugFindingEntity.BugFindingStatus mapStatus(BugFinding.BugFindingStatus status) {
        if (status == null) {
            return BugFindingEntity.BugFindingStatus.DETECTED;
        }
        return BugFindingEntity.BugFindingStatus.valueOf(status.name());
    }
    
    private BugFinding.BugFindingStatus mapStatus(BugFindingEntity.BugFindingStatus status) {
        if (status == null) {
            return BugFinding.BugFindingStatus.DETECTED;
        }
        return BugFinding.BugFindingStatus.valueOf(status.name());
    }
}

