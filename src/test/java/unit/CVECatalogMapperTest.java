package com.bugbounty.cve.mapper;

import com.bugbounty.cve.domain.CVECatalog;
import com.bugbounty.cve.entity.CVECatalogEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CVECatalogMapper Tests")
class CVECatalogMapperTest {

    private CVECatalogMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CVECatalogMapper();
    }

    @Test
    @DisplayName("Should map domain to entity")
    void shouldMapDomainToEntity() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        CVECatalog catalog = CVECatalog.builder()
                .id(id)
                .cveId("CVE-2024-1234")
                .language("Java")
                .summary("Test summary")
                .codeExample("vulnerable code")
                .vulnerablePattern("pattern description")
                .fixedPattern("fixed pattern")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        CVECatalogEntity entity = mapper.toEntity(catalog);

        // Then
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("CVE-2024-1234", entity.getCveId());
        assertEquals("Java", entity.getLanguage());
        assertEquals("Test summary", entity.getSummary());
        assertEquals("vulnerable code", entity.getCodeExample());
        assertEquals("pattern description", entity.getVulnerablePattern());
        assertEquals("fixed pattern", entity.getFixedPattern());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    @DisplayName("Should map entity to domain")
    void shouldMapEntityToDomain() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        CVECatalogEntity entity = CVECatalogEntity.builder()
                .id(id)
                .cveId("CVE-2024-1234")
                .language("Java")
                .summary("Test summary")
                .codeExample("vulnerable code")
                .vulnerablePattern("pattern description")
                .fixedPattern("fixed pattern")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // When
        CVECatalog catalog = mapper.toDomain(entity);

        // Then
        assertNotNull(catalog);
        assertEquals(id, catalog.getId());
        assertEquals("CVE-2024-1234", catalog.getCveId());
        assertEquals("Java", catalog.getLanguage());
        assertEquals("Test summary", catalog.getSummary());
        assertEquals("vulnerable code", catalog.getCodeExample());
        assertEquals("pattern description", catalog.getVulnerablePattern());
        assertEquals("fixed pattern", catalog.getFixedPattern());
        assertEquals(now, catalog.getCreatedAt());
        assertEquals(now, catalog.getUpdatedAt());
    }

    @Test
    @DisplayName("Should handle null domain")
    void shouldHandleNullDomain() {
        // When
        CVECatalogEntity entity = mapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    @DisplayName("Should handle null entity")
    void shouldHandleNullEntity() {
        // When
        CVECatalog catalog = mapper.toDomain(null);

        // Then
        assertNull(catalog);
    }
}

