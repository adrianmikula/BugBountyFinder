package com.bugbounty.repository.service;

import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.entity.RepositoryEntity;
import com.bugbounty.repository.mapper.RepositoryMapper;
import com.bugbounty.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing repository registration and retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryManagementService {
    
    private final RepositoryRepository repositoryRepository;
    private final RepositoryMapper repositoryMapper;
    
    /**
     * Add a new repository to monitor.
     * 
     * @param repository The repository domain object to add
     * @return The saved repository entity
     * @throws IllegalArgumentException if repository URL already exists
     */
    @Transactional
    public RepositoryEntity addRepository(Repository repository) {
        log.info("Adding repository: {}", repository.getUrl());
        
        // Validate URL is provided
        if (repository.getUrl() == null || repository.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Repository URL is required");
        }
        
        // Check if repository already exists
        if (repositoryRepository.existsByUrl(repository.getUrl())) {
            throw new IllegalArgumentException("Repository already exists: " + repository.getUrl());
        }
        
        // Ensure owner and name are extracted from URL
        String owner = repository.getOwner();
        String name = repository.getName();
        
        // Validate that URL could be parsed
        if (owner == null || name == null) {
            throw new IllegalArgumentException(
                "Invalid GitHub repository URL. Expected format: https://github.com/owner/repo");
        }
        
        // Convert to entity and save
        RepositoryEntity entity = repositoryMapper.toEntity(repository);
        RepositoryEntity saved = repositoryRepository.save(entity);
        
        log.info("Successfully added repository: {} (ID: {})", saved.getUrl(), saved.getId());
        return saved;
    }
    
    /**
     * Get all registered repositories.
     */
    public List<Repository> getAllRepositories() {
        return repositoryRepository.findAll().stream()
                .map(repositoryMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    /**
     * Get repository by URL.
     */
    public Optional<Repository> getRepositoryByUrl(String url) {
        return repositoryRepository.findByUrl(url)
                .map(repositoryMapper::toDomain);
    }
    
    /**
     * Get repository by ID.
     */
    public Optional<Repository> getRepositoryById(java.util.UUID id) {
        return repositoryRepository.findById(id)
                .map(repositoryMapper::toDomain);
    }
    
    /**
     * Delete a repository by URL.
     */
    @Transactional
    public boolean deleteRepository(String url) {
        Optional<RepositoryEntity> entity = repositoryRepository.findByUrl(url);
        if (entity.isPresent()) {
            repositoryRepository.delete(entity.get());
            log.info("Deleted repository: {}", url);
            return true;
        }
        return false;
    }
}

