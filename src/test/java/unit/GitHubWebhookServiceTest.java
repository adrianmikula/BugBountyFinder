package com.bugbounty.webhook.service;

import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.service.RepositoryService;
import com.bugbounty.webhook.dto.GitHubPushEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubWebhookService Tests")
class GitHubWebhookServiceTest {

    @Mock
    private RepositoryService repositoryService;

    @InjectMocks
    private GitHubWebhookService webhookService;

    private GitHubPushEvent createValidPushEvent() {
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/heads/main");
        
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setId(123456L);
        repo.setName("test-repo");
        repo.setFullName("owner/test-repo");
        repo.setCloneUrl("https://github.com/owner/test-repo.git");
        repo.setHtmlUrl("https://github.com/owner/test-repo");
        repo.setDefaultBranch("main");
        pushEvent.setRepository(repo);
        
        GitHubPushEvent.Commit commit = new GitHubPushEvent.Commit();
        commit.setId("abc123def456");
        commit.setMessage("Fix bug");
        commit.setTimestamp("2024-01-01T12:00:00Z");
        commit.setAdded(Arrays.asList("file1.java"));
        commit.setModified(Arrays.asList("file2.java"));
        commit.setRemoved(Collections.emptyList());
        pushEvent.setCommits(Arrays.asList(commit));
        
        GitHubPushEvent.Pusher pusher = new GitHubPushEvent.Pusher();
        pusher.setName("testuser");
        pushEvent.setPusher(pusher);
        
        return pushEvent;
    }

    @Test
    @DisplayName("Should process valid push event successfully")
    void shouldProcessValidPushEvent() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(true);
        doNothing().when(repositoryService).updateRepository(any(Repository.class));
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertTrue(result);
        verify(repositoryService, times(1)).isCloned(any(Repository.class));
        verify(repositoryService, times(1)).updateRepository(any(Repository.class));
    }

    @Test
    @DisplayName("Should handle repository not yet cloned")
    void shouldHandleRepositoryNotCloned() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(false);
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertTrue(result); // Should still return true, just log that it's not cloned
        verify(repositoryService, times(1)).isCloned(any(Repository.class));
        verify(repositoryService, never()).updateRepository(any(Repository.class));
    }

    @Test
    @DisplayName("Should return false for null push event")
    void shouldReturnFalseForNullPushEvent() {
        // When
        boolean result = webhookService.processPushEvent(null);
        
        // Then
        assertFalse(result);
        verify(repositoryService, never()).isCloned(any());
    }

    @Test
    @DisplayName("Should return false for push event without repository")
    void shouldReturnFalseForPushEventWithoutRepository() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRepository(null);
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for push event without clone URL")
    void shouldReturnFalseForPushEventWithoutCloneUrl() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setCloneUrl(null);
        pushEvent.setRepository(repo);
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle update repository exception gracefully")
    void shouldHandleUpdateRepositoryException() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(true);
        doThrow(new RuntimeException("Update failed"))
                .when(repositoryService).updateRepository(any(Repository.class));
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertFalse(result);
        verify(repositoryService, times(1)).updateRepository(any(Repository.class));
    }

    @Test
    @DisplayName("Should extract branch name correctly")
    void shouldExtractBranchName() {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.setRef("refs/heads/feature-branch");
        
        // When
        String branchName = pushEvent.getBranchName();
        
        // Then
        assertEquals("feature-branch", branchName);
    }

    @Test
    @DisplayName("Should return null for invalid ref format")
    void shouldReturnNullForInvalidRefFormat() {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.setRef("invalid-ref");
        
        // When
        String branchName = pushEvent.getBranchName();
        
        // Then
        assertNull(branchName);
    }

    @Test
    @DisplayName("Should check if push is to default branch")
    void shouldCheckIfPushIsToDefaultBranch() {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.setRef("refs/heads/main");
        pushEvent.getRepository().setDefaultBranch("main");
        
        // When
        boolean isDefault = pushEvent.isDefaultBranch();
        
        // Then
        assertTrue(isDefault);
    }

    @Test
    @DisplayName("Should handle multiple commits in push event")
    void shouldHandleMultipleCommits() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        
        GitHubPushEvent.Commit commit1 = new GitHubPushEvent.Commit();
        commit1.setId("commit1");
        commit1.setMessage("First commit");
        
        GitHubPushEvent.Commit commit2 = new GitHubPushEvent.Commit();
        commit2.setId("commit2");
        commit2.setMessage("Second commit");
        
        pushEvent.setCommits(Arrays.asList(commit1, commit2));
        
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(true);
        doNothing().when(repositoryService).updateRepository(any(Repository.class));
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertTrue(result);
        assertEquals(2, pushEvent.getCommits().size());
    }

    @Test
    @DisplayName("Should handle invalid repository URL format")
    void shouldHandleInvalidRepositoryUrlFormat() {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.getRepository().setCloneUrl("invalid-url-format");
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertFalse(result);
        verify(repositoryService, never()).isCloned(any());
    }

    @Test
    @DisplayName("Should handle SSH URL format")
    void shouldHandleSshUrlFormat() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.getRepository().setCloneUrl("git@github.com:owner/test-repo.git");
        
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(true);
        doNothing().when(repositoryService).updateRepository(any(Repository.class));
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertTrue(result);
        verify(repositoryService, times(1)).isCloned(any(Repository.class));
    }

    @Test
    @DisplayName("Should handle HTTP URL format")
    void shouldHandleHttpUrlFormat() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.getRepository().setCloneUrl("http://github.com/owner/test-repo.git");
        
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(true);
        doNothing().when(repositoryService).updateRepository(any(Repository.class));
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertTrue(result);
        verify(repositoryService, times(1)).isCloned(any(Repository.class));
    }

    @Test
    @DisplayName("Should handle empty commits list")
    void shouldHandleEmptyCommitsList() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.setCommits(Collections.emptyList());
        
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(true);
        doNothing().when(repositoryService).updateRepository(any(Repository.class));
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertTrue(result);
        assertTrue(pushEvent.getCommits().isEmpty());
    }

    @Test
    @DisplayName("Should handle null commits list")
    void shouldHandleNullCommitsList() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.setCommits(null);
        
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(true);
        doNothing().when(repositoryService).updateRepository(any(Repository.class));
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertTrue(result);
        assertNull(pushEvent.getCommits());
    }

    @Test
    @DisplayName("Should check if repository is tracked")
    void shouldCheckIfRepositoryIsTracked() {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        
        // When
        boolean isTracked = webhookService.isTrackedRepository(pushEvent);
        
        // Then
        assertTrue(isTracked); // Currently accepts all repositories
    }

    @Test
    @DisplayName("Should handle repository with null default branch")
    void shouldHandleRepositoryWithNullDefaultBranch() throws Exception {
        // Given
        GitHubPushEvent pushEvent = createValidPushEvent();
        pushEvent.getRepository().setDefaultBranch(null);
        
        when(repositoryService.isCloned(any(Repository.class))).thenReturn(true);
        doNothing().when(repositoryService).updateRepository(any(Repository.class));
        
        // When
        boolean result = webhookService.processPushEvent(pushEvent);
        
        // Then
        assertTrue(result);
    }
}

