package com.bugbounty.bounty.mapper;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.entity.BountyEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BountyMapper Tests")
class BountyMapperTest {

    private BountyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BountyMapper();
    }

    @Test
    @DisplayName("Should convert domain to entity with all fields")
    void shouldConvertDomainToEntity() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Bounty bounty = Bounty.builder()
                .id(id)
                .issueId("issue-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .title("Fix bug")
                .description("Bug description")
                .status(BountyStatus.OPEN)
                .createdAt(now)
                .startedAt(now.plusHours(1))
                .completedAt(now.plusHours(2))
                .failedAt(now.plusHours(3))
                .pullRequestId("pr-123")
                .failureReason("Test failure")
                .build();

        // When
        BountyEntity entity = mapper.toEntity(bounty);

        // Then
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("issue-123", entity.getIssueId());
        assertEquals("https://github.com/owner/repo", entity.getRepositoryUrl());
        assertEquals("algora", entity.getPlatform());
        assertEquals(new BigDecimal("150.00"), entity.getAmount());
        assertEquals("USD", entity.getCurrency());
        assertEquals("Fix bug", entity.getTitle());
        assertEquals("Bug description", entity.getDescription());
        assertEquals(BountyStatus.OPEN, entity.getStatus());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now.plusHours(1), entity.getStartedAt());
        assertEquals(now.plusHours(2), entity.getCompletedAt());
        assertEquals(now.plusHours(3), entity.getFailedAt());
        assertEquals("pr-123", entity.getPullRequestId());
        assertEquals("Test failure", entity.getFailureReason());
    }

    @Test
    @DisplayName("Should return null when converting null domain to entity")
    void shouldReturnNullWhenDomainIsNull() {
        // When
        BountyEntity entity = mapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    @DisplayName("Should convert entity to domain with all fields")
    void shouldConvertEntityToDomain() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        BountyEntity entity = BountyEntity.builder()
                .id(id)
                .issueId("issue-456")
                .repositoryUrl("https://github.com/owner/repo2")
                .platform("polar")
                .amount(new BigDecimal("200.00"))
                .currency("EUR")
                .title("Fix another bug")
                .description("Another bug description")
                .status(BountyStatus.IN_PROGRESS)
                .createdAt(now)
                .startedAt(now.plusHours(1))
                .completedAt(now.plusHours(2))
                .failedAt(now.plusHours(3))
                .pullRequestId("pr-456")
                .failureReason("Another failure")
                .build();

        // When
        Bounty bounty = mapper.toDomain(entity);

        // Then
        assertNotNull(bounty);
        assertEquals(id, bounty.getId());
        assertEquals("issue-456", bounty.getIssueId());
        assertEquals("https://github.com/owner/repo2", bounty.getRepositoryUrl());
        assertEquals("polar", bounty.getPlatform());
        assertEquals(new BigDecimal("200.00"), bounty.getAmount());
        assertEquals("EUR", bounty.getCurrency());
        assertEquals("Fix another bug", bounty.getTitle());
        assertEquals("Another bug description", bounty.getDescription());
        assertEquals(BountyStatus.IN_PROGRESS, bounty.getStatus());
        assertEquals(now, bounty.getCreatedAt());
        assertEquals(now.plusHours(1), bounty.getStartedAt());
        assertEquals(now.plusHours(2), bounty.getCompletedAt());
        assertEquals(now.plusHours(3), bounty.getFailedAt());
        assertEquals("pr-456", bounty.getPullRequestId());
        assertEquals("Another failure", bounty.getFailureReason());
    }

    @Test
    @DisplayName("Should return null when converting null entity to domain")
    void shouldReturnNullWhenEntityIsNull() {
        // When
        Bounty bounty = mapper.toDomain(null);

        // Then
        assertNull(bounty);
    }

    @Test
    @DisplayName("Should handle domain with null optional fields")
    void shouldHandleDomainWithNullOptionalFields() {
        // Given
        Bounty bounty = Bounty.builder()
                .id(UUID.randomUUID())
                .issueId("issue-789")
                .platform("algora")
                .amount(new BigDecimal("100.00"))
                .status(BountyStatus.OPEN)
                .build();

        // When
        BountyEntity entity = mapper.toEntity(bounty);

        // Then
        assertNotNull(entity);
        assertEquals("issue-789", entity.getIssueId());
        assertNull(entity.getRepositoryUrl());
        assertNull(entity.getCurrency());
        assertNull(entity.getTitle());
        assertNull(entity.getDescription());
        assertNull(entity.getStartedAt());
        assertNull(entity.getCompletedAt());
        assertNull(entity.getFailedAt());
        assertNull(entity.getPullRequestId());
        assertNull(entity.getFailureReason());
    }

    @Test
    @DisplayName("Should handle entity with null optional fields")
    void shouldHandleEntityWithNullOptionalFields() {
        // Given
        BountyEntity entity = BountyEntity.builder()
                .id(UUID.randomUUID())
                .issueId("issue-999")
                .platform("polar")
                .amount(new BigDecimal("50.00"))
                .status(BountyStatus.OPEN)
                .build();

        // When
        Bounty bounty = mapper.toDomain(entity);

        // Then
        assertNotNull(bounty);
        assertEquals("issue-999", bounty.getIssueId());
        assertNull(bounty.getRepositoryUrl());
        assertNull(bounty.getCurrency());
        assertNull(bounty.getTitle());
        assertNull(bounty.getDescription());
        assertNull(bounty.getStartedAt());
        assertNull(bounty.getCompletedAt());
        assertNull(bounty.getFailedAt());
        assertNull(bounty.getPullRequestId());
        assertNull(bounty.getFailureReason());
    }
}

