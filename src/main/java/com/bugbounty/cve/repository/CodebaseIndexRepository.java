package com.bugbounty.cve.repository;

import com.bugbounty.cve.entity.CodebaseIndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodebaseIndexRepository extends JpaRepository<CodebaseIndexEntity, UUID> {
    
    Optional<CodebaseIndexEntity> findByRepositoryUrlAndLanguage(String repositoryUrl, String language);
    
    Optional<CodebaseIndexEntity> findByRepositoryUrl(String repositoryUrl);
}

