package com.bugbounty.cve.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for mapping CVE affected products/languages to repository programming languages.
 * This helps determine which repositories should be scanned when a CVE is published.
 */
@Service
public class LanguageMappingService {
    
    // Map of common product names/frameworks to programming languages
    private static final Map<String, List<String>> PRODUCT_TO_LANGUAGE_MAP = Map.ofEntries(
            // Java ecosystem
            Map.entry("spring", Arrays.asList("Java")),
            Map.entry("spring framework", Arrays.asList("Java")),
            Map.entry("spring boot", Arrays.asList("Java")),
            Map.entry("apache log4j", Arrays.asList("Java")),
            Map.entry("apache struts", Arrays.asList("Java")),
            Map.entry("jackson", Arrays.asList("Java")),
            Map.entry("hibernate", Arrays.asList("Java")),
            Map.entry("maven", Arrays.asList("Java")),
            Map.entry("gradle", Arrays.asList("Java", "Kotlin")),
            Map.entry("kotlin", Arrays.asList("Kotlin", "Java")),
            
            // Python ecosystem
            Map.entry("django", Arrays.asList("Python")),
            Map.entry("flask", Arrays.asList("Python")),
            Map.entry("fastapi", Arrays.asList("Python")),
            Map.entry("numpy", Arrays.asList("Python")),
            Map.entry("pandas", Arrays.asList("Python")),
            Map.entry("requests", Arrays.asList("Python")),
            Map.entry("pip", Arrays.asList("Python")),
            
            // JavaScript/Node.js ecosystem
            Map.entry("node.js", Arrays.asList("JavaScript", "TypeScript")),
            Map.entry("express", Arrays.asList("JavaScript", "TypeScript")),
            Map.entry("react", Arrays.asList("JavaScript", "TypeScript")),
            Map.entry("vue", Arrays.asList("JavaScript", "TypeScript")),
            Map.entry("angular", Arrays.asList("TypeScript", "JavaScript")),
            Map.entry("npm", Arrays.asList("JavaScript", "TypeScript")),
            Map.entry("yarn", Arrays.asList("JavaScript", "TypeScript")),
            Map.entry("webpack", Arrays.asList("JavaScript", "TypeScript")),
            
            // .NET ecosystem
            Map.entry("asp.net", Arrays.asList("C#")),
            Map.entry("entity framework", Arrays.asList("C#")),
            Map.entry("nuget", Arrays.asList("C#")),
            
            // Go ecosystem
            Map.entry("golang", Arrays.asList("Go")),
            Map.entry("go", Arrays.asList("Go")),
            
            // Ruby ecosystem
            Map.entry("rails", Arrays.asList("Ruby")),
            Map.entry("ruby on rails", Arrays.asList("Ruby")),
            Map.entry("bundler", Arrays.asList("Ruby")),
            
            // PHP ecosystem
            Map.entry("laravel", Arrays.asList("PHP")),
            Map.entry("symfony", Arrays.asList("PHP")),
            Map.entry("composer", Arrays.asList("PHP")),
            
            // Rust
            Map.entry("rust", Arrays.asList("Rust")),
            Map.entry("cargo", Arrays.asList("Rust")),
            
            // C/C++
            Map.entry("openssl", Arrays.asList("C", "C++")),
            Map.entry("libcurl", Arrays.asList("C", "C++"))
    );
    
    // Direct language name mappings (case-insensitive)
    private static final Set<String> KNOWN_LANGUAGES = Set.of(
            "Java", "Python", "JavaScript", "TypeScript", "Ruby", "PHP", 
            "Go", "Rust", "C", "C++", "C#", "Swift", "Kotlin", "Scala"
    );
    
    /**
     * Extract programming languages from CVE affected products and languages.
     * 
     * @param affectedProducts List of affected product names
     * @param affectedLanguages List of affected languages (if already extracted)
     * @return Set of programming languages that should be scanned
     */
    public Set<String> extractLanguages(List<String> affectedProducts, List<String> affectedLanguages) {
        Set<String> languages = new HashSet<>();
        
        // Add directly specified languages
        if (affectedLanguages != null) {
            for (String lang : affectedLanguages) {
                String normalized = normalizeLanguage(lang);
                if (normalized != null) {
                    languages.add(normalized);
                }
            }
        }
        
        // Extract languages from product names
        if (affectedProducts != null) {
            for (String product : affectedProducts) {
                String normalizedProduct = product.toLowerCase().trim();
                
                // Check direct product mappings
                for (Map.Entry<String, List<String>> entry : PRODUCT_TO_LANGUAGE_MAP.entrySet()) {
                    if (normalizedProduct.contains(entry.getKey())) {
                        languages.addAll(entry.getValue());
                    }
                }
                
                // Check if product name itself is a language
                String lang = normalizeLanguage(product);
                if (lang != null) {
                    languages.add(lang);
                }
            }
        }
        
        return languages;
    }
    
    /**
     * Normalize language name to standard format.
     */
    private String normalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return null;
        }
        
        String normalized = language.trim();
        
        // Direct matches
        for (String knownLang : KNOWN_LANGUAGES) {
            if (knownLang.equalsIgnoreCase(normalized)) {
                return knownLang;
            }
        }
        
        // Common aliases
        return switch (normalized.toLowerCase()) {
            case "js", "node", "nodejs" -> "JavaScript";
            case "ts" -> "TypeScript";
            case "cpp", "cxx" -> "C++";
            case "csharp", "cs" -> "C#";
            case "golang" -> "Go";
            default -> null;
        };
    }
    
    /**
     * Check if a repository language matches any of the CVE affected languages.
     */
    public boolean matchesLanguage(String repositoryLanguage, Set<String> cveLanguages) {
        if (repositoryLanguage == null || cveLanguages == null || cveLanguages.isEmpty()) {
            return false;
        }
        
        String normalizedRepoLang = normalizeLanguage(repositoryLanguage);
        if (normalizedRepoLang == null) {
            return false;
        }
        
        return cveLanguages.contains(normalizedRepoLang);
    }
}

