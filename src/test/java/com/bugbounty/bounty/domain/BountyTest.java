package com.bugbounty.bounty.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bounty Domain Model Tests")
class BountyTest {

    @Test
    @DisplayName("Should create a valid bounty with all required fields")
    void shouldCreateValidBounty() {
        // Given
        String issueId = "issue-123";
        String repositoryUrl = "https://github.com/owner/repo";
        String platform = "algora";
        BigDecimal amount = new BigDecimal("150.00");
        String currency = "USD";
        String title = "Fix React hydration error";
        String description = "The component has a hydration mismatch";
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        Bounty bounty = Bounty.builder()
                .id(UUID.randomUUID())
                .issueId(issueId)
                .repositoryUrl(repositoryUrl)
                .platform(platform)
                .amount(amount)
                .currency(currency)
                .title(title)
                .description(description)
                .status(BountyStatus.OPEN)
                .createdAt(createdAt)
                .build();

        // Then
        assertNotNull(bounty.getId());
        assertEquals(issueId, bounty.getIssueId());
        assertEquals(repositoryUrl, bounty.getRepositoryUrl());
        assertEquals(platform, bounty.getPlatform());
        assertEquals(amount, bounty.getAmount());
        assertEquals(currency, bounty.getCurrency());
        assertEquals(title, bounty.getTitle());
        assertEquals(description, bounty.getDescription());
        assertEquals(BountyStatus.OPEN, bounty.getStatus());
        assertEquals(createdAt, bounty.getCreatedAt());
    }

    @Test
    @DisplayName("Should create bounty with minimum required fields")
    void shouldCreateBountyWithMinimumFields() {
        // Given
        String issueId = "issue-456";
        String repositoryUrl = "https://github.com/owner/repo";
        String platform = "polar";

        // When
        Bounty bounty = Bounty.builder()
                .issueId(issueId)
                .repositoryUrl(repositoryUrl)
                .platform(platform)
                .status(BountyStatus.OPEN)
                .build();

        // Then
        assertNotNull(bounty.getId());
        assertEquals(issueId, bounty.getIssueId());
        assertEquals(repositoryUrl, bounty.getRepositoryUrl());
        assertEquals(platform, bounty.getPlatform());
        assertEquals(BountyStatus.OPEN, bounty.getStatus());
        assertNull(bounty.getAmount());
    }

    @Test
    @DisplayName("Should transition bounty status from OPEN to IN_PROGRESS")
    void shouldTransitionStatusToInProgress() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .status(BountyStatus.OPEN)
                .build();

        // When
        bounty.markInProgress();

        // Then
        assertEquals(BountyStatus.IN_PROGRESS, bounty.getStatus());
        assertNotNull(bounty.getStartedAt());
    }

    @Test
    @DisplayName("Should transition bounty status to COMPLETED")
    void shouldTransitionStatusToCompleted() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .status(BountyStatus.IN_PROGRESS)
                .build();

        // When
        bounty.markCompleted("pr-123");

        // Then
        assertEquals(BountyStatus.COMPLETED, bounty.getStatus());
        assertEquals("pr-123", bounty.getPullRequestId());
        assertNotNull(bounty.getCompletedAt());
    }

    @Test
    @DisplayName("Should transition bounty status to FAILED")
    void shouldTransitionStatusToFailed() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .status(BountyStatus.IN_PROGRESS)
                .build();

        // When
        bounty.markFailed("Unable to reproduce issue");

        // Then
        assertEquals(BountyStatus.FAILED, bounty.getStatus());
        assertEquals("Unable to reproduce issue", bounty.getFailureReason());
        assertNotNull(bounty.getFailedAt());
    }

    @Test
    @DisplayName("Should check if bounty is eligible for processing")
    void shouldCheckIfBountyIsEligible() {
        // Given
        Bounty openBounty = Bounty.builder()
                .issueId("issue-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .status(BountyStatus.OPEN)
                .amount(new BigDecimal("100.00"))
                .build();

        Bounty inProgressBounty = Bounty.builder()
                .issueId("issue-456")
                .repositoryUrl("https://github.com/owner/repo2")
                .platform("polar")
                .status(BountyStatus.IN_PROGRESS)
                .build();

        // Then
        assertTrue(openBounty.isEligibleForProcessing());
        assertFalse(inProgressBounty.isEligibleForProcessing());
    }

    @Test
    @DisplayName("Should calculate minimum bounty amount threshold")
    void shouldRespectMinimumBountyAmount() {
        // Given
        Bounty highValueBounty = Bounty.builder()
                .issueId("issue-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        Bounty lowValueBounty = Bounty.builder()
                .issueId("issue-456")
                .repositoryUrl("https://github.com/owner/repo2")
                .platform("polar")
                .amount(new BigDecimal("25.00"))
                .status(BountyStatus.OPEN)
                .build();

        BigDecimal minimumAmount = new BigDecimal("50.00");

        // Then
        assertTrue(highValueBounty.meetsMinimumAmount(minimumAmount));
        assertFalse(lowValueBounty.meetsMinimumAmount(minimumAmount));
    }
}

