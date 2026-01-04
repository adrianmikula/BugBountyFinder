package com.bugbounty.cve.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model for language-specific CVE catalog entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVECatalog {
    
    @Builder.Default
    private UUID id = UUID.randomUUID();
    
    private String cveId;
    private String language; // e.g., "Java", "Python", "JavaScript"
    private String summary; // LLM-generated summary
    private String codeExample; // Example vulnerable code
    private String vulnerablePattern; // Pattern description
    private String fixedPattern; // Fixed code pattern
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

