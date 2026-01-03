package com.bugbounty.bounty.mapper;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.entity.BountyEntity;
import org.springframework.stereotype.Component;

@Component
public class BountyMapper {

    public BountyEntity toEntity(Bounty domain) {
        if (domain == null) {
            return null;
        }

        return BountyEntity.builder()
                .id(domain.getId())
                .issueId(domain.getIssueId())
                .repositoryUrl(domain.getRepositoryUrl())
                .platform(domain.getPlatform())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .startedAt(domain.getStartedAt())
                .completedAt(domain.getCompletedAt())
                .failedAt(domain.getFailedAt())
                .pullRequestId(domain.getPullRequestId())
                .failureReason(domain.getFailureReason())
                .build();
    }

    public Bounty toDomain(BountyEntity entity) {
        if (entity == null) {
            return null;
        }

        return Bounty.builder()
                .id(entity.getId())
                .issueId(entity.getIssueId())
                .repositoryUrl(entity.getRepositoryUrl())
                .platform(entity.getPlatform())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .failedAt(entity.getFailedAt())
                .pullRequestId(entity.getPullRequestId())
                .failureReason(entity.getFailureReason())
                .build();
    }
}

