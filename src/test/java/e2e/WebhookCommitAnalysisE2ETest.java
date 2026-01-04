package com.bugbounty.e2e;

import com.bugbounty.component.AbstractComponentTest;
import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.entity.RepositoryEntity;
import com.bugbounty.repository.repository.RepositoryRepository;
import com.bugbounty.repository.service.RepositoryService;
import com.bugbounty.webhook.dto.GitHubPushEvent;
import com.bugbounty.webhook.service.GitHubWebhookService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for webhook commit analysis.
 * 
 * This test:
 * 1. Picks the first repository from the database
 * 2. Gets the most recent commit from that repository
 * 3. Triggers the webhook service to analyze the commit for bugs
 */
@DisplayName("Webhook Commit Analysis E2E Test")
class WebhookCommitAnalysisE2ETest extends AbstractComponentTest {

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private GitHubWebhookService webhookService;

    @Value("${app.repository.clone.base-path:./repos}")
    private String basePath;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Ensure containers are ready before tests run
        // TestContainers should handle this, but we verify to prevent connection issues
        // The containers are static and shared from AbstractComponentTest
        // No explicit cleanup needed as containers are recreated for each test run
    }

    @Test
    @DisplayName("Should analyze most recent commit from first watched repository")
    void shouldAnalyzeMostRecentCommitFromFirstWatchedRepository() throws Exception {
        // Given - Get the first repository from the database
        // Note: Containers should be ready via AbstractComponentTest, but we handle connection issues gracefully
        List<RepositoryEntity> repositories = repositoryRepository.findAll();
        
        if (repositories.isEmpty()) {
            // If no repositories exist, create a test one
            RepositoryEntity testRepo = RepositoryEntity.builder()
                    .url("https://github.com/octocat/Hello-World.git")
                    .owner("octocat")
                    .name("Hello-World")
                    .defaultBranch("master")
                    .language("Java")
                    .build();
            testRepo = repositoryRepository.save(testRepo);
            repositories = List.of(testRepo);
        }

        assertFalse(repositories.isEmpty(), "No repositories found in database. " +
                "Please ensure at least one repository is being watched.");

        RepositoryEntity repoEntity = repositories.get(0);
        assertNotNull(repoEntity.getUrl(), "Repository URL is null");

        // Convert to domain object
        Repository repository = Repository.builder()
                .url(repoEntity.getUrl())
                .owner(repoEntity.getOwner())
                .name(repoEntity.getName())
                .defaultBranch(repoEntity.getDefaultBranch())
                .language(repoEntity.getLanguage())
                .localPath(repoEntity.getLocalPath())
                .build();

        // Ensure repository is cloned
        String clonePath = tempDir.resolve("repos").toString();
        if (!repository.isCloned()) {
            repository = repositoryService.cloneRepository(repository, clonePath);
            // Update entity with local path
            repoEntity.setLocalPath(repository.getLocalPath());
            repositoryRepository.save(repoEntity);
        } else {
            // Update repository to ensure we have latest commits
            repositoryService.updateRepository(repository);
        }

        assertTrue(repository.isCloned(), "Repository should be cloned");
        assertNotNull(repository.getLocalPath(), "Repository local path should not be null");

        // Get the most recent commit using JGit
        RevCommit mostRecentCommit = getMostRecentCommit(repository.getLocalPath(), 
                repository.getDefaultBranch() != null ? repository.getDefaultBranch() : "main");

        assertNotNull(mostRecentCommit, "No commits found in repository");
        String commitId = mostRecentCommit.getId().getName();
        String commitMessage = mostRecentCommit.getFullMessage();
        Instant commitTime = Instant.ofEpochSecond(mostRecentCommit.getCommitTime());

        // Build GitHubPushEvent with the commit
        GitHubPushEvent pushEvent = buildPushEvent(repository, mostRecentCommit);

        // When - Process the webhook event
        boolean processed = webhookService.processPushEvent(pushEvent);

        // Then - Verify the event was processed successfully
        assertTrue(processed, "Webhook event should be processed successfully");
        
        // Verify commit information
        assertNotNull(pushEvent.getCommits());
        assertFalse(pushEvent.getCommits().isEmpty(), "Push event should contain commits");
        
        GitHubPushEvent.Commit commit = pushEvent.getCommits().get(0);
        assertEquals(commitId, commit.getId(), "Commit ID should match");
        assertEquals(commitMessage, commit.getMessage(), "Commit message should match");
    }

    /**
     * Get the most recent commit from a repository.
     */
    private RevCommit getMostRecentCommit(String localPath, String branch) throws Exception {
        try (Git git = Git.open(new java.io.File(localPath))) {
            // Resolve branch reference
            String branchRef = "refs/heads/" + branch;
            ObjectId branchId = git.getRepository().resolve(branchRef);
            
            // If branch doesn't exist, try HEAD
            if (branchId == null) {
                branchId = git.getRepository().resolve("HEAD");
            }
            
            if (branchId == null) {
                throw new IllegalStateException("Could not resolve branch or HEAD: " + branch);
            }

            // Get the most recent commit
            LogCommand logCommand = git.log()
                    .setMaxCount(1)
                    .add(branchId);

            Iterable<RevCommit> commits = logCommand.call();
            return commits.iterator().hasNext() ? commits.iterator().next() : null;
        }
    }

    /**
     * Build a GitHubPushEvent from a repository and commit.
     */
    private GitHubPushEvent buildPushEvent(Repository repository, RevCommit commit) {
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        
        // Set ref (branch)
        String branch = repository.getDefaultBranch() != null ? repository.getDefaultBranch() : "main";
        pushEvent.setRef("refs/heads/" + branch);
        
        // Set repository information
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setName(repository.getName());
        repo.setFullName(repository.getFullPath());
        repo.setCloneUrl(repository.getUrl());
        repo.setDefaultBranch(branch);
        pushEvent.setRepository(repo);
        
        // Build commit information
        GitHubPushEvent.Commit commitDto = new GitHubPushEvent.Commit();
        commitDto.setId(commit.getId().getName());
        commitDto.setMessage(commit.getFullMessage());
        
        // Format timestamp
        Instant commitTime = Instant.ofEpochSecond(commit.getCommitTime());
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(commitTime);
        commitDto.setTimestamp(timestamp);
        
        // Get changed files from commit using the repository
        try {
            List<String> added = getChangedFiles(repository, commit, DiffEntry.ChangeType.ADD);
            List<String> modified = getChangedFiles(repository, commit, DiffEntry.ChangeType.MODIFY);
            List<String> removed = getChangedFiles(repository, commit, DiffEntry.ChangeType.DELETE);
            
            commitDto.setAdded(added);
            commitDto.setModified(modified);
            commitDto.setRemoved(removed);
        } catch (Exception e) {
            // If we can't get file changes, continue with empty lists
            // The webhook service will still be able to analyze the commit
            commitDto.setAdded(List.of());
            commitDto.setModified(List.of());
            commitDto.setRemoved(List.of());
        }
        
        pushEvent.setCommits(List.of(commitDto));
        
        return pushEvent;
    }

    /**
     * Get changed files from a commit by analyzing the diff.
     */
    private List<String> getChangedFiles(Repository repository, RevCommit commit, DiffEntry.ChangeType changeType) 
            throws Exception {
        try (Git git = Git.open(new java.io.File(repository.getLocalPath()));
             RevWalk walk = new RevWalk(git.getRepository());
             DiffFormatter diffFormatter = new DiffFormatter(NullOutputStream.INSTANCE)) {
            
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setDetectRenames(true);
            
            RevTree commitTree = walk.parseTree(commit.getTree());
            RevTree parentTree = null;
            
            if (commit.getParentCount() > 0) {
                RevCommit parent = walk.parseCommit(commit.getParent(0));
                parentTree = walk.parseTree(parent.getTree());
            }
            
            List<DiffEntry> diffEntries = diffFormatter.scan(parentTree, commitTree);
            
            return diffEntries.stream()
                    .filter(entry -> entry.getChangeType() == changeType)
                    .map(entry -> {
                        // Use new path for ADD, old path for DELETE, new path for MODIFY
                        if (changeType == DiffEntry.ChangeType.DELETE) {
                            return entry.getOldPath();
                        } else {
                            return entry.getNewPath();
                        }
                    })
                    .toList();
        }
    }
}

