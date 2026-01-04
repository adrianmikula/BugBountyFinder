package com.bugbounty.cve.mapper;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.entity.CVEEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CVEMapper Tests")
class CVEMapperTest {

    private CVEMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CVEMapper(new ObjectMapper());
    }

    @Test
    @DisplayName("Should convert domain to entity with all fields")
    void shouldConvertDomainToEntity() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        List<String> languages = Arrays.asList("Java", "Python");
        List<String> products = Arrays.asList("Spring Framework", "Spring Boot");

        CVE cve = CVE.builder()
                .id(id)
                .cveId("CVE-2024-1234")
                .description("Vulnerability in Spring Framework")
                .severity("CRITICAL")
                .cvssScore(9.8)
                .publishedDate(now)
                .lastModifiedDate(now.plusHours(1))
                .affectedLanguages(languages)
                .affectedProducts(products)
                .source("NVD")
                .build();

        // When
        CVEEntity entity = mapper.toEntity(cve);

        // Then
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("CVE-2024-1234", entity.getCveId());
        assertEquals("Vulnerability in Spring Framework", entity.getDescription());
        assertEquals("CRITICAL", entity.getSeverity());
        assertEquals(9.8, entity.getCvssScore());
        assertEquals(now, entity.getPublishedDate());
        assertEquals(now.plusHours(1), entity.getLastModifiedDate());
        assertEquals("NVD", entity.getSource());
        assertNotNull(entity.getAffectedLanguages());
        assertNotNull(entity.getAffectedProducts());
    }

    @Test
    @DisplayName("Should return null when converting null domain to entity")
    void shouldReturnNullWhenDomainIsNull() {
        // When
        CVEEntity entity = mapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    @DisplayName("Should convert entity to domain with all fields")
    void shouldConvertEntityToDomain() {
        // Given
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String languagesJson = "[\"Java\",\"Python\"]";
        String productsJson = "[\"Spring Framework\",\"Spring Boot\"]";

        CVEEntity entity = CVEEntity.builder()
                .id(id)
                .cveId("CVE-2024-5678")
                .description("Another vulnerability")
                .severity("HIGH")
                .cvssScore(7.5)
                .publishedDate(now)
                .lastModifiedDate(now.plusHours(2))
                .affectedLanguages(languagesJson)
                .affectedProducts(productsJson)
                .source("WEBHOOK")
                .build();

        // When
        CVE cve = mapper.toDomain(entity);

        // Then
        assertNotNull(cve);
        assertEquals(id, cve.getId());
        assertEquals("CVE-2024-5678", cve.getCveId());
        assertEquals("Another vulnerability", cve.getDescription());
        assertEquals("HIGH", cve.getSeverity());
        assertEquals(7.5, cve.getCvssScore());
        assertEquals(now, cve.getPublishedDate());
        assertEquals(now.plusHours(2), cve.getLastModifiedDate());
        assertEquals("WEBHOOK", cve.getSource());
        assertNotNull(cve.getAffectedLanguages());
        assertEquals(2, cve.getAffectedLanguages().size());
        assertTrue(cve.getAffectedLanguages().contains("Java"));
        assertTrue(cve.getAffectedLanguages().contains("Python"));
        assertNotNull(cve.getAffectedProducts());
        assertEquals(2, cve.getAffectedProducts().size());
    }

    @Test
    @DisplayName("Should return null when converting null entity to domain")
    void shouldReturnNullWhenEntityIsNull() {
        // When
        CVE cve = mapper.toDomain(null);

        // Then
        assertNull(cve);
    }

    @Test
    @DisplayName("Should handle domain with null optional fields")
    void shouldHandleDomainWithNullOptionalFields() {
        // Given
        CVE cve = CVE.builder()
                .id(UUID.randomUUID())
                .cveId("CVE-2024-9999")
                .severity("MEDIUM")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        // When
        CVEEntity entity = mapper.toEntity(cve);

        // Then
        assertNotNull(entity);
        assertEquals("CVE-2024-9999", entity.getCveId());
        assertNull(entity.getDescription());
        assertNull(entity.getCvssScore());
        assertNull(entity.getLastModifiedDate());
        assertEquals("[]", entity.getAffectedLanguages());
        assertEquals("[]", entity.getAffectedProducts());
    }

    @Test
    @DisplayName("Should handle entity with null optional fields")
    void shouldHandleEntityWithNullOptionalFields() {
        // Given
        CVEEntity entity = CVEEntity.builder()
                .id(UUID.randomUUID())
                .cveId("CVE-2024-8888")
                .severity("LOW")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedLanguages(null)
                .affectedProducts(null)
                .build();

        // When
        CVE cve = mapper.toDomain(entity);

        // Then
        assertNotNull(cve);
        assertEquals("CVE-2024-8888", cve.getCveId());
        assertNull(cve.getDescription());
        assertNull(cve.getCvssScore());
        assertNull(cve.getLastModifiedDate());
        assertNotNull(cve.getAffectedLanguages());
        assertTrue(cve.getAffectedLanguages().isEmpty());
        assertNotNull(cve.getAffectedProducts());
        assertTrue(cve.getAffectedProducts().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty lists in JSON conversion")
    void shouldHandleEmptyListsInJsonConversion() {
        // Given
        CVE cve = CVE.builder()
                .id(UUID.randomUUID())
                .cveId("CVE-2024-7777")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedLanguages(Arrays.asList())
                .affectedProducts(Arrays.asList())
                .build();

        // When
        CVEEntity entity = mapper.toEntity(cve);
        CVE convertedBack = mapper.toDomain(entity);

        // Then
        assertNotNull(convertedBack);
        assertNotNull(convertedBack.getAffectedLanguages());
        assertTrue(convertedBack.getAffectedLanguages().isEmpty());
        assertNotNull(convertedBack.getAffectedProducts());
        assertTrue(convertedBack.getAffectedProducts().isEmpty());
    }

    @Test
    @DisplayName("Should correctly serialize and deserialize lists")
    void shouldCorrectlySerializeAndDeserializeLists() {
        // Given
        List<String> languages = Arrays.asList("Java", "Python", "JavaScript");
        List<String> products = Arrays.asList("Spring", "Django", "React");

        CVE cve = CVE.builder()
                .id(UUID.randomUUID())
                .cveId("CVE-2024-6666")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedLanguages(languages)
                .affectedProducts(products)
                .build();

        // When
        CVEEntity entity = mapper.toEntity(cve);
        CVE convertedBack = mapper.toDomain(entity);

        // Then
        assertNotNull(convertedBack);
        assertEquals(3, convertedBack.getAffectedLanguages().size());
        assertEquals(3, convertedBack.getAffectedProducts().size());
        assertTrue(convertedBack.getAffectedLanguages().containsAll(languages));
        assertTrue(convertedBack.getAffectedProducts().containsAll(products));
    }
}

