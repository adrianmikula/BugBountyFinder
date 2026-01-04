package com.bugbounty.cve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cves", indexes = {
    @Index(name = "idx_cve_id", columnList = "cveId", unique = true),
    @Index(name = "idx_severity", columnList = "severity"),
    @Index(name = "idx_published_date", columnList = "publishedDate")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVEEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String cveId;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    private Double cvssScore;

    @Column(nullable = false)
    private LocalDateTime publishedDate;

    private LocalDateTime lastModifiedDate;

    @Column(columnDefinition = "TEXT")
    private String affectedLanguages; // JSON array as string

    @Column(columnDefinition = "TEXT")
    private String affectedProducts; // JSON array as string

    @Column(nullable = false)
    private String source; // "NVD", "GITHUB", "WEBHOOK"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

