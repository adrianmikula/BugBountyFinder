package com.bugbounty.webhook.service;

import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.service.RepositoryService;
import com.bugbounty.webhook.dto.GitHubPushEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
                pushEvent.getCommits().forEach(commit -> {
                    log.debug("Commit: {} - {} (added: {}, modified: {}, removed: {})",
                            commit.getId().substring(0, Math.min(7, commit.getId().length())),
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
}

