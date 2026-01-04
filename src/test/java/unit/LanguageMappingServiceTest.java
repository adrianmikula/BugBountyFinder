package com.bugbounty.cve.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LanguageMappingService Tests")
class LanguageMappingServiceTest {

    private LanguageMappingService service;

    @BeforeEach
    void setUp() {
        service = new LanguageMappingService();
    }

    @Test
    @DisplayName("Should extract Java language from Spring Framework product")
    void shouldExtractJavaFromSpringFramework() {
        // Given
        List<String> products = Arrays.asList("Spring Framework", "Spring Boot");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("Java"));
    }

    @Test
    @DisplayName("Should extract Python language from Django product")
    void shouldExtractPythonFromDjango() {
        // Given
        List<String> products = Arrays.asList("Django", "Flask");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("Python"));
    }

    @Test
    @DisplayName("Should extract JavaScript from Node.js product")
    void shouldExtractJavaScriptFromNodeJs() {
        // Given
        List<String> products = Arrays.asList("Node.js", "Express");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("JavaScript"));
        assertTrue(languages.contains("TypeScript"));
    }

    @Test
    @DisplayName("Should extract multiple languages from multiple products")
    void shouldExtractMultipleLanguagesFromMultipleProducts() {
        // Given
        List<String> products = Arrays.asList("Spring Framework", "Django", "React");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("Java"));
        assertTrue(languages.contains("Python"));
        assertTrue(languages.contains("JavaScript"));
        assertTrue(languages.contains("TypeScript"));
    }

    @Test
    @DisplayName("Should use directly specified languages")
    void shouldUseDirectlySpecifiedLanguages() {
        // Given
        List<String> products = Arrays.asList("Unknown Product");
        List<String> languages = Arrays.asList("Java", "Python");

        // When
        Set<String> extracted = service.extractLanguages(products, languages);

        // Then
        assertNotNull(extracted);
        assertTrue(extracted.contains("Java"));
        assertTrue(extracted.contains("Python"));
    }

    @Test
    @DisplayName("Should combine product-based and direct languages")
    void shouldCombineProductBasedAndDirectLanguages() {
        // Given
        List<String> products = Arrays.asList("Spring Framework");
        List<String> languages = Arrays.asList("Python");

        // When
        Set<String> extracted = service.extractLanguages(products, languages);

        // Then
        assertNotNull(extracted);
        assertTrue(extracted.contains("Java"));
        assertTrue(extracted.contains("Python"));
    }

    @Test
    @DisplayName("Should return empty set when no languages can be extracted")
    void shouldReturnEmptySetWhenNoLanguagesExtracted() {
        // Given
        List<String> products = Arrays.asList("Unknown Product XYZ");
        List<String> languages = null;

        // When
        Set<String> extracted = service.extractLanguages(products, languages);

        // Then
        assertNotNull(extracted);
        assertTrue(extracted.isEmpty());
    }

    @Test
    @DisplayName("Should handle null products list")
    void shouldHandleNullProductsList() {
        // Given
        List<String> languages = Arrays.asList("Java");

        // When
        Set<String> extracted = service.extractLanguages(null, languages);

        // Then
        assertNotNull(extracted);
        assertTrue(extracted.contains("Java"));
    }

    @Test
    @DisplayName("Should handle null languages list")
    void shouldHandleNullLanguagesList() {
        // Given
        List<String> products = Arrays.asList("Spring Framework");

        // When
        Set<String> extracted = service.extractLanguages(products, null);

        // Then
        assertNotNull(extracted);
        assertTrue(extracted.contains("Java"));
    }

    @Test
    @DisplayName("Should match repository language with CVE languages")
    void shouldMatchRepositoryLanguageWithCVELanguages() {
        // Given
        String repoLanguage = "Java";
        Set<String> cveLanguages = new HashSet<>(Arrays.asList("Java", "Python"));

        // When
        boolean matches = service.matchesLanguage(repoLanguage, cveLanguages);

        // Then
        assertTrue(matches);
    }

    @Test
    @DisplayName("Should not match when repository language is not in CVE languages")
    void shouldNotMatchWhenRepositoryLanguageNotInCVELanguages() {
        // Given
        String repoLanguage = "Ruby";
        Set<String> cveLanguages = new HashSet<>(Arrays.asList("Java", "Python"));

        // When
        boolean matches = service.matchesLanguage(repoLanguage, cveLanguages);

        // Then
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should handle case-insensitive language matching")
    void shouldHandleCaseInsensitiveLanguageMatching() {
        // Given
        String repoLanguage = "java";
        Set<String> cveLanguages = new HashSet<>(Arrays.asList("Java"));

        // When
        boolean matches = service.matchesLanguage(repoLanguage, cveLanguages);

        // Then
        assertTrue(matches);
    }

    @Test
    @DisplayName("Should return false when repository language is null")
    void shouldReturnFalseWhenRepositoryLanguageIsNull() {
        // Given
        String repoLanguage = null;
        Set<String> cveLanguages = new HashSet<>(Arrays.asList("Java"));

        // When
        boolean matches = service.matchesLanguage(repoLanguage, cveLanguages);

        // Then
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should return false when CVE languages is null")
    void shouldReturnFalseWhenCVELanguagesIsNull() {
        // Given
        String repoLanguage = "Java";
        Set<String> cveLanguages = null;

        // When
        boolean matches = service.matchesLanguage(repoLanguage, cveLanguages);

        // Then
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should return false when CVE languages is empty")
    void shouldReturnFalseWhenCVELanguagesIsEmpty() {
        // Given
        String repoLanguage = "Java";
        Set<String> cveLanguages = new HashSet<>();

        // When
        boolean matches = service.matchesLanguage(repoLanguage, cveLanguages);

        // Then
        assertFalse(matches);
    }

    @Test
    @DisplayName("Should extract Go language from golang product")
    void shouldExtractGoFromGolang() {
        // Given
        List<String> products = Arrays.asList("golang", "Go");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("Go"));
    }

    @Test
    @DisplayName("Should extract C# language from ASP.NET product")
    void shouldExtractCSharpFromAspNet() {
        // Given
        List<String> products = Arrays.asList("ASP.NET", "Entity Framework");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("C#"));
    }

    @Test
    @DisplayName("Should extract Ruby language from Rails product")
    void shouldExtractRubyFromRails() {
        // Given
        List<String> products = Arrays.asList("Rails", "Ruby on Rails");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("Ruby"));
    }

    @Test
    @DisplayName("Should extract PHP language from Laravel product")
    void shouldExtractPHPFromLaravel() {
        // Given
        List<String> products = Arrays.asList("Laravel", "Symfony");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("PHP"));
    }

    @Test
    @DisplayName("Should extract Rust language from Rust product")
    void shouldExtractRustFromRust() {
        // Given
        List<String> products = Arrays.asList("Rust", "Cargo");

        // When
        Set<String> languages = service.extractLanguages(products, null);

        // Then
        assertNotNull(languages);
        assertTrue(languages.contains("Rust"));
    }

    @Test
    @DisplayName("Should normalize language aliases")
    void shouldNormalizeLanguageAliases() {
        // Given
        List<String> products = Arrays.asList("Node.js");
        List<String> languages = Arrays.asList("js", "ts", "cpp", "csharp", "golang");

        // When
        Set<String> extracted = service.extractLanguages(products, languages);

        // Then
        assertNotNull(extracted);
        assertTrue(extracted.contains("JavaScript"));
        assertTrue(extracted.contains("TypeScript"));
        assertTrue(extracted.contains("C++"));
        assertTrue(extracted.contains("C#"));
        assertTrue(extracted.contains("Go"));
    }
}

