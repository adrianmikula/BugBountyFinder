package com.bugbounty.cve.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CVE Domain Model Tests")
class CVETest {

    @Test
    @DisplayName("Should create a valid CVE with all required fields")
    void shouldCreateValidCVE() {
        // Given
        String cveId = "CVE-2024-1234";
        String description = "Vulnerability in Spring Framework";
        String severity = "CRITICAL";
        Double cvssScore = 9.8;
        LocalDateTime publishedDate = LocalDateTime.now();
        LocalDateTime lastModifiedDate = LocalDateTime.now();
        List<String> affectedLanguages = Arrays.asList("Java");
        List<String> affectedProducts = Arrays.asList("Spring Framework", "Spring Boot");
        String source = "NVD";

        // When
        CVE cve = CVE.builder()
                .id(UUID.randomUUID())
                .cveId(cveId)
                .description(description)
                .severity(severity)
                .cvssScore(cvssScore)
                .publishedDate(publishedDate)
                .lastModifiedDate(lastModifiedDate)
                .affectedLanguages(affectedLanguages)
                .affectedProducts(affectedProducts)
                .source(source)
                .build();

        // Then
        assertNotNull(cve.getId());
        assertEquals(cveId, cve.getCveId());
        assertEquals(description, cve.getDescription());
        assertEquals(severity, cve.getSeverity());
        assertEquals(cvssScore, cve.getCvssScore());
        assertEquals(publishedDate, cve.getPublishedDate());
        assertEquals(lastModifiedDate, cve.getLastModifiedDate());
        assertEquals(affectedLanguages, cve.getAffectedLanguages());
        assertEquals(affectedProducts, cve.getAffectedProducts());
        assertEquals(source, cve.getSource());
    }

    @Test
    @DisplayName("Should create CVE with minimum required fields")
    void shouldCreateCVEWithMinimumFields() {
        // Given
        String cveId = "CVE-2024-5678";

        // When
        CVE cve = CVE.builder()
                .cveId(cveId)
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        // Then
        assertNotNull(cve.getId());
        assertEquals(cveId, cve.getCveId());
        assertNull(cve.getDescription());
        assertNull(cve.getCvssScore());
        assertNull(cve.getAffectedLanguages());
        assertNull(cve.getAffectedProducts());
    }

    @Test
    @DisplayName("Should identify CRITICAL severity as critical or high")
    void shouldIdentifyCriticalAsCriticalOrHigh() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        // Then
        assertTrue(cve.isCriticalOrHigh());
    }

    @Test
    @DisplayName("Should identify HIGH severity as critical or high")
    void shouldIdentifyHighAsCriticalOrHigh() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        // Then
        assertTrue(cve.isCriticalOrHigh());
    }

    @Test
    @DisplayName("Should not identify MEDIUM severity as critical or high")
    void shouldNotIdentifyMediumAsCriticalOrHigh() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("MEDIUM")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        // Then
        assertFalse(cve.isCriticalOrHigh());
    }

    @Test
    @DisplayName("Should not identify LOW severity as critical or high")
    void shouldNotIdentifyLowAsCriticalOrHigh() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("LOW")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        // Then
        assertFalse(cve.isCriticalOrHigh());
    }

    @Test
    @DisplayName("Should check if CVE affects a specific language")
    void shouldCheckIfCVEAffectsLanguage() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedLanguages(Arrays.asList("Java", "Python"))
                .build();

        // Then
        assertTrue(cve.affectsLanguage("Java"));
        assertTrue(cve.affectsLanguage("Python"));
        assertFalse(cve.affectsLanguage("JavaScript"));
        assertFalse(cve.affectsLanguage(null));
    }

    @Test
    @DisplayName("Should handle case-insensitive language matching")
    void shouldHandleCaseInsensitiveLanguageMatching() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedLanguages(Arrays.asList("Java"))
                .build();

        // Then
        assertTrue(cve.affectsLanguage("java"));
        assertTrue(cve.affectsLanguage("JAVA"));
        assertTrue(cve.affectsLanguage("Java"));
    }

    @Test
    @DisplayName("Should return false when affected languages is null")
    void shouldReturnFalseWhenAffectedLanguagesIsNull() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedLanguages(null)
                .build();

        // Then
        assertFalse(cve.affectsLanguage("Java"));
    }

    @Test
    @DisplayName("Should generate UUID when id is not provided")
    void shouldGenerateUuidWhenIdNotProvided() {
        // When
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        // Then
        assertNotNull(cve.getId());
    }

    @Test
    @DisplayName("Should handle empty affected languages list")
    void shouldHandleEmptyAffectedLanguages() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedLanguages(Arrays.asList())
                .build();

        // Then
        assertFalse(cve.affectsLanguage("Java"));
    }
}

