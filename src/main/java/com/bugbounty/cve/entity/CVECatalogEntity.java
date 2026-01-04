package com.bugbounty.cve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing language-specific CVE summaries and code examples.
 * One record per CVE per programming language.
 */
@Entity
@Table(name = "cve_catalog", indexes = {
    @Index(name = "idx_cve_catalog_cve_language", columnList = "cveId,language"),
    @Index(name = "idx_cve_catalog_language", columnList = "language")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVECatalogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String cveId;

    @Column(nullable = false, length = 100)
    private String language; // e.g., "Java", "Python", "JavaScript"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary; // LLM-generated summary of the CVE for this language

    @Column(columnDefinition = "TEXT")
    private String codeExample; // Example of vulnerable code pattern

    @Column(columnDefinition = "TEXT")
    private String vulnerablePattern; // Pattern description of vulnerable code

    @Column(columnDefinition = "TEXT")
    private String fixedPattern; // Pattern description of fixed code

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

