package com.bugbounty.cve.mapper;

import com.bugbounty.cve.domain.BugFinding;
import com.bugbounty.cve.entity.BugFindingEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BugFindingMapper Tests")
class BugFindingMapperTest {

    private BugFindingMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mapper = new BugFindingMapper(objectMapper);
    }

    @Test
    @DisplayName("Should map domain to entity")
    void shouldMapDomainToEntity() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        BugFinding finding = BugFinding.builder()
                .id(id)
                .repositoryUrl("https://github.com/owner/repo")
                .commitId("abc123")
                .cveId("CVE-2024-1234")
                .status(BugFinding.BugFindingStatus.DETECTED)
                .presenceConfidence(0.8)
                .fixConfidence(0.9)
                .commitDiff("diff content")
                .affectedFiles(List.of("file1.java", "file2.java"))
                .recommendedFix("fix code")
                .verificationNotes("notes")
                .requiresHumanReview(false)
                .humanReviewed(false)
                .humanReviewNotes("review notes")
                .pullRequestId("pr-123")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        BugFindingEntity entity = mapper.toEntity(finding);

        // Then
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("https://github.com/owner/repo", entity.getRepositoryUrl());
        assertEquals("abc123", entity.getCommitId());
        assertEquals("CVE-2024-1234", entity.getCveId());
        assertEquals(BugFindingEntity.BugFindingStatus.DETECTED, entity.getStatus());
        assertEquals(0.8, entity.getPresenceConfidence());
        assertEquals(0.9, entity.getFixConfidence());
        assertEquals("diff content", entity.getCommitDiff());
        assertNotNull(entity.getAffectedFiles()); // Should be JSON string
        assertEquals("fix code", entity.getRecommendedFix());
        assertEquals("notes", entity.getVerificationNotes());
        assertFalse(entity.getRequiresHumanReview());
        assertFalse(entity.getHumanReviewed());
        assertEquals("review notes", entity.getHumanReviewNotes());
        assertEquals("pr-123", entity.getPullRequestId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("Should map entity to domain")
    void shouldMapEntityToDomain() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        BugFindingEntity entity = BugFindingEntity.builder()
                .id(id)
                .repositoryUrl("https://github.com/owner/repo")
                .commitId("abc123")
                .cveId("CVE-2024-1234")
                .status(BugFindingEntity.BugFindingStatus.DETECTED)
                .presenceConfidence(0.8)
                .fixConfidence(0.9)
                .commitDiff("diff content")
                .affectedFiles("[\"file1.java\", \"file2.java\"]")
                .recommendedFix("fix code")
                .verificationNotes("notes")
                .requiresHumanReview(false)
                .humanReviewed(false)
                .humanReviewNotes("review notes")
                .pullRequestId("pr-123")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        BugFinding finding = mapper.toDomain(entity);

        // Then
        assertNotNull(finding);
        assertEquals(id, finding.getId());
        assertEquals("https://github.com/owner/repo", finding.getRepositoryUrl());
        assertEquals("abc123", finding.getCommitId());
        assertEquals("CVE-2024-1234", finding.getCveId());
        assertEquals(BugFinding.BugFindingStatus.DETECTED, finding.getStatus());
        assertEquals(0.8, finding.getPresenceConfidence());
        assertEquals(0.9, finding.getFixConfidence());
        assertEquals("diff content", finding.getCommitDiff());
        assertNotNull(finding.getAffectedFiles());
        assertEquals(2, finding.getAffectedFiles().size());
        assertEquals("file1.java", finding.getAffectedFiles().get(0));
        assertEquals("fix code", finding.getRecommendedFix());
        assertEquals("notes", finding.getVerificationNotes());
        assertFalse(finding.getRequiresHumanReview());
        assertFalse(finding.getHumanReviewed());
        assertEquals("review notes", finding.getHumanReviewNotes());
        assertEquals("pr-123", finding.getPullRequestId());
        assertEquals(now, finding.getCreatedAt());
        assertEquals(now, finding.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle null domain")
    void shouldHandleNullDomain() {
        // When
        BugFindingEntity entity = mapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    @DisplayName("Should handle null entity")
    void shouldHandleNullEntity() {
        // When
        BugFinding finding = mapper.toDomain(null);

        // Then
        assertNull(finding);
    }

    @Test
    @DisplayName("Should handle null affected files")
    void shouldHandleNullAffectedFiles() {
        // Given
        BugFinding finding = BugFinding.builder()
                .repositoryUrl("https://github.com/owner/repo")
                .commitId("abc123")
                .cveId("CVE-2024-1234")
                .status(BugFinding.BugFindingStatus.DETECTED)
                .affectedFiles(null)
                .build();

        // When
        BugFindingEntity entity = mapper.toEntity(finding);

        // Then
        assertNotNull(entity);
        assertNull(entity.getAffectedFiles());
    }

    @Test
    @DisplayName("Should handle empty affected files JSON")
    void shouldHandleEmptyAffectedFiles() {
        // Given
        BugFindingEntity entity = BugFindingEntity.builder()
                .repositoryUrl("https://github.com/owner/repo")
                .commitId("abc123")
                .cveId("CVE-2024-1234")
                .status(BugFindingEntity.BugFindingStatus.DETECTED)
                .affectedFiles("")
                .build();

        // When
        BugFinding finding = mapper.toDomain(entity);

        // Then
        assertNotNull(finding);
        assertNull(finding.getAffectedFiles());
    }
}

