package com.bugbounty.repository.repository;

import com.bugbounty.repository.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositoryRepository extends JpaRepository<RepositoryEntity, UUID> {
    
    boolean existsByUrl(String url);
    
    Optional<RepositoryEntity> findByUrl(String url);
    
    List<RepositoryEntity> findByLanguage(String language);
    
    List<RepositoryEntity> findByLanguageIn(List<String> languages);
}

