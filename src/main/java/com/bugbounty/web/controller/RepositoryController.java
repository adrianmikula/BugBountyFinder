package com.bugbounty.web.controller;

import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.dto.AddRepositoryRequest;
import com.bugbounty.repository.entity.RepositoryEntity;
import com.bugbounty.repository.service.RepositoryManagementService;
import com.bugbounty.web.dto.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing repositories.
 */
@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
@Slf4j
public class RepositoryController {
    
    private final RepositoryManagementService repositoryManagementService;
    
    /**
     * Add a new repository to monitor.
     * 
     * @param request The repository information
     * @return The created repository entity
     */
    @PostMapping
    public ResponseEntity<?> addRepository(@Valid @RequestBody AddRepositoryRequest request) {
        log.info("Received request to add repository: {}", request.getUrl());
        
        try {
            Repository repository = Repository.builder()
                    .url(request.getUrl())
                    .language(request.getLanguage())
                    .defaultBranch(request.getDefaultBranch())
                    .build();
            
            RepositoryEntity saved = repositoryManagementService.addRepository(repository);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to add repository: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Error adding repository", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to add repository: " + e.getMessage()));
        }
    }
    
    /**
     * Handle validation errors from @Valid annotation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }
    
    /**
     * Get all registered repositories.
     */
    @GetMapping
    public ResponseEntity<List<Repository>> getAllRepositories() {
        log.debug("Fetching all repositories");
        List<Repository> repositories = repositoryManagementService.getAllRepositories();
        return ResponseEntity.ok(repositories);
    }
    
    /**
     * Get repository by URL.
     */
    @GetMapping("/by-url")
    public ResponseEntity<Repository> getRepositoryByUrl(@RequestParam String url) {
        log.debug("Fetching repository by URL: {}", url);
        return repositoryManagementService.getRepositoryByUrl(url)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get repository by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Repository> getRepositoryById(@PathVariable UUID id) {
        log.debug("Fetching repository by ID: {}", id);
        return repositoryManagementService.getRepositoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Delete a repository by URL.
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteRepository(@RequestParam String url) {
        log.info("Deleting repository: {}", url);
        boolean deleted = repositoryManagementService.deleteRepository(url);
        return deleted 
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

