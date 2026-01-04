package com.bugbounty.webhook.service;

import com.bugbounty.cve.service.CommitAnalysisService;
import com.bugbounty.cve.service.CodebaseIndexService;
import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.entity.RepositoryEntity;
import com.bugbounty.repository.repository.RepositoryRepository;
import com.bugbounty.repository.service.RepositoryService;
import com.bugbounty.webhook.dto.GitHubPushEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for processing GitHub webhook events.
 * Handles push events and triggers repository updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubWebhookService {
    
    private static final Pattern GITHUB_URL_PATTERN = 
        Pattern.compile("(?:https?://|git@)github\\.com[:/]([^/]+)/([^/\\.]+)(?:\\.git)?");
    
    private final RepositoryService repositoryService;
    private final RepositoryRepository repositoryRepository;
    private final CommitAnalysisService commitAnalysisService;
    private final CodebaseIndexService codebaseIndexService;
    
    @Value("${app.repository.clone.base-path:./repos}")
    private String basePath;
    
    /**
     * Process a GitHub push event.
     * 
     * @param pushEvent The push event from GitHub webhook
     * @return true if event was processed successfully, false otherwise
     */
    public boolean processPushEvent(GitHubPushEvent pushEvent) {
        if (pushEvent == null || pushEvent.getRepository() == null) {
            log.warn("Invalid push event: missing repository information");
            return false;
        }
        
        String repositoryUrl = pushEvent.getRepository().getCloneUrl();
        if (repositoryUrl == null || repositoryUrl.isEmpty()) {
            log.warn("Invalid push event: missing clone URL");
            return false;
        }
        
        log.info("Processing push event for repository: {} (branch: {})", 
                pushEvent.getRepository().getFullName(),
                pushEvent.getBranchName());
        
        try {
            // Extract owner and name from repository
            Matcher matcher = GITHUB_URL_PATTERN.matcher(repositoryUrl);
            if (!matcher.find()) {
                log.warn("Could not parse repository URL: {}", repositoryUrl);
                return false;
            }
            
            String owner = matcher.group(1);
            String name = matcher.group(2);
            
            // Create or find repository domain object
            Repository repository = Repository.builder()
                    .url(repositoryUrl)
                    .owner(owner)
                    .name(name)
                    .defaultBranch(pushEvent.getRepository().getDefaultBranch())
                    .build();
            
            // Check if repository is already cloned
            if (repositoryService.isCloned(repository)) {
                log.info("Updating existing repository: {}", repository.getFullPath());
                repositoryService.updateRepository(repository);
            } else {
                log.info("Repository not yet cloned: {}. Clone will happen on next polling cycle.", 
                        repository.getFullPath());
                // Repository will be cloned when bounty is processed
                // For now, we just log that we received the push event
            }
            
            // Log commit information
            if (pushEvent.getCommits() != null && !pushEvent.getCommits().isEmpty()) {
                log.info("Received {} commit(s) in push event", pushEvent.getCommits().size());
                
                // Analyze commits for CVE vulnerabilities
                analyzeCommitsForCVEs(repository, pushEvent);
                
                pushEvent.getCommits().forEach(commit -> {
                    String commitId = commit.getId();
                    String shortId = (commitId != null && commitId.length() > 0) 
                            ? commitId.substring(0, Math.min(7, commitId.length()))
                            : "unknown";
                    log.debug("Commit: {} - {} (added: {}, modified: {}, removed: {})",
                            shortId,
                            commit.getMessage(),
                            commit.getAdded() != null ? commit.getAdded().size() : 0,
                            commit.getModified() != null ? commit.getModified().size() : 0,
                            commit.getRemoved() != null ? commit.getRemoved().size() : 0);
                });
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error processing push event for repository: {}", 
                    pushEvent.getRepository().getFullName(), e);
            return false;
        }
    }
    
    /**
     * Check if the push event is for a tracked repository.
     * This can be enhanced to check against a database of tracked repositories.
     */
    public boolean isTrackedRepository(GitHubPushEvent pushEvent) {
        // For now, we accept all repositories
        // In the future, this could check against a database
        return true;
    }
    
    /**
     * Analyze commits for CVE vulnerabilities.
     */
    private void analyzeCommitsForCVEs(Repository repository, GitHubPushEvent pushEvent) {
        if (pushEvent.getCommits() == null || pushEvent.getCommits().isEmpty()) {
            return;
        }
        
        // Get repository language from database
        Optional<RepositoryEntity> repoEntity = repositoryRepository.findByUrl(repository.getUrl());
        if (repoEntity.isEmpty() || repoEntity.get().getLanguage() == null) {
            log.debug("Repository language not set, skipping CVE analysis: {}", repository.getUrl());
            return;
        }
        
        String language = repoEntity.get().getLanguage();
        log.info("Analyzing commits for CVE vulnerabilities in repository {} (language: {})", 
                repository.getUrl(), language);
        
        // Ensure repository is cloned and indexed
        if (!repository.isCloned()) {
            try {
                repository = repositoryService.cloneRepository(repository, basePath);
            } catch (Exception e) {
                log.error("Failed to clone repository for CVE analysis: {}", repository.getUrl(), e);
                return;
            }
        }
        
        // Create/update codebase index
        try {
            codebaseIndexService.indexRepository(repository, language);
        } catch (Exception e) {
            log.warn("Failed to index repository, continuing with analysis: {}", repository.getUrl(), e);
        }
        
        // Analyze each commit
        for (GitHubPushEvent.Commit commit : pushEvent.getCommits()) {
            try {
                // Get commit diff
                String commitDiff = repositoryService.getCommitDiff(repository, commit.getId());
                
                // Collect affected files
                List<String> affectedFiles = new ArrayList<>();
                if (commit.getAdded() != null) {
                    affectedFiles.addAll(commit.getAdded());
                }
                if (commit.getModified() != null) {
                    affectedFiles.addAll(commit.getModified());
                }
                if (commit.getRemoved() != null) {
                    affectedFiles.addAll(commit.getRemoved());
                }
                
                // Analyze commit for CVEs
                commitAnalysisService.analyzeCommit(
                        repository.getUrl(),
                        commit.getId(),
                        commitDiff,
                        affectedFiles,
                        language
                )
                .doOnNext(finding -> log.info("Found potential CVE {} in commit {} (confidence: {})",
                        finding.getCveId(), commit.getId(), finding.getPresenceConfidence()))
                .doOnError(error -> log.error("Error analyzing commit {} for CVEs", commit.getId(), error))
                .subscribe();
                
            } catch (Exception e) {
                log.error("Error analyzing commit {} for CVEs", commit.getId(), e);
            }
        }
    }
}

