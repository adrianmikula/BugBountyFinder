package com.bugbounty.cve.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVE {
    
    @Builder.Default
    private UUID id = UUID.randomUUID();
    
    private String cveId; // e.g., "CVE-2024-1234"
    private String description;
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private Double cvssScore;
    private LocalDateTime publishedDate;
    private LocalDateTime lastModifiedDate;
    private List<String> affectedLanguages; // e.g., ["Java", "Python", "JavaScript"]
    private List<String> affectedProducts; // e.g., ["Spring Framework", "Apache Log4j"]
    private String source; // "NVD", "GITHUB", "WEBHOOK"
    
    public boolean isCriticalOrHigh() {
        return "CRITICAL".equalsIgnoreCase(severity) || "HIGH".equalsIgnoreCase(severity);
    }
    
    public boolean affectsLanguage(String language) {
        if (affectedLanguages == null || language == null) {
            return false;
        }
        return affectedLanguages.stream()
                .anyMatch(lang -> lang.equalsIgnoreCase(language));
    }
}

