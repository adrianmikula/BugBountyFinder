package com.bugbounty.cve.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing codebase structure index.
 * Contains lightweight index of classes, methods, packages for LLM context.
 */
@Entity
@Table(name = "codebase_index", indexes = {
    @Index(name = "idx_codebase_index_repo", columnList = "repositoryUrl"),
    @Index(name = "idx_codebase_index_repo_lang", columnList = "repositoryUrl,language")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodebaseIndexEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String repositoryUrl;

    @Column(nullable = false, length = 100)
    private String language; // e.g., "Java", "Python", "JavaScript"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String indexData; // JSON structure: packages, classes, methods, etc.

    @Column(nullable = false)
    @Builder.Default
    private Integer indexVersion = 1; // Increment when index structure changes

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

