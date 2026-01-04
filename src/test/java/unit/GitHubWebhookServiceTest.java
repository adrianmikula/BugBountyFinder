package com.bugbounty.webhook.service;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.service.GitHubIssueScannerService;
import com.bugbounty.cve.service.CommitAnalysisService;
import com.bugbounty.cve.service.CodebaseIndexService;
import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.repository.RepositoryRepository;
import com.bugbounty.repository.service.RepositoryService;
import com.bugbounty.webhook.dto.GitHubIssueEvent;
import com.bugbounty.webhook.dto.GitHubPushEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubWebhookService Tests")
class GitHubWebhookServiceTest {

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private CommitAnalysisService commitAnalysisService;

    @Mock
    private CodebaseIndexService codebaseIndexService;

    @Mock
    private GitHubIssueScannerService githubIssueScannerService;

    @InjectMocks
    private GitHubWebhookService webhookService;

    @BeforeEach
    void setUp() throws Exception {
        // Default mocks for all tests - can be overridden in individual tests
        // Using lenient() because these mocks may not be used in all tests
        // Mock repository not found in DB (no language set, so CVE analysis is skipped)
        lenient().when(repositoryRepository.findByUrl(anyString())).thenReturn(Optional.empty());
        // Mock commit analysis to return empty flux (no CVEs found)
        lenient().when(commitAnalysisService.analyzeCommit(anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(Flux.empty());
        // Mock getCommitDiff to return empty diff (in case CVE analysis is triggered)
        lenient().when(repositoryService.getCommitDiff(any(Repository.class), anyString())).thenReturn("");
    }

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
        // Mock repository not found in DB (no language set, so CVE analysis is skipped)
        when(repositoryRepository.findByUrl(anyString())).thenReturn(Optional.empty());
        
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

    // Issue Event Tests

    private GitHubIssueEvent createValidIssueEvent() {
        GitHubIssueEvent issueEvent = new GitHubIssueEvent();
        issueEvent.setAction("opened");
        
        GitHubIssueEvent.Issue issue = new GitHubIssueEvent.Issue();
        issue.setId(123456L);
        issue.setNumber(42);
        issue.setTitle("Fix security vulnerability - $500 bounty");
        issue.setBody("This issue has a $500 bounty attached.");
        issue.setState("open");
        issue.setCreatedAt("2024-01-01T12:00:00Z");
        issue.setUpdatedAt("2024-01-01T12:00:00Z");
        issue.setPullRequest(null); // This is an issue, not a PR
        issueEvent.setIssue(issue);
        
        GitHubIssueEvent.Repository repo = new GitHubIssueEvent.Repository();
        repo.setId(789012L);
        repo.setName("test-repo");
        repo.setFullName("owner/test-repo");
        repo.setCloneUrl("https://github.com/owner/test-repo.git");
        repo.setHtmlUrl("https://github.com/owner/test-repo");
        repo.setDefaultBranch("main");
        issueEvent.setRepository(repo);
        
        return issueEvent;
    }

    @Test
    @DisplayName("Should process valid opened issue event successfully")
    void shouldProcessValidOpenedIssueEvent() {
        // Given
        GitHubIssueEvent issueEvent = createValidIssueEvent();
        Bounty bounty = Bounty.builder()
                .issueId("42")
                .repositoryUrl("https://github.com/owner/test-repo")
                .platform("github")
                .amount(new java.math.BigDecimal("500.00"))
                .title("Fix security vulnerability - $500 bounty")
                .build();
        
        when(githubIssueScannerService.processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(bounty);
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertTrue(result);
        verify(githubIssueScannerService, times(1)).processIssueFromWebhook(
                eq("https://github.com/owner/test-repo"),
                eq("42"),
                eq("Fix security vulnerability - $500 bounty"),
                eq("This issue has a $500 bounty attached."));
    }

    @Test
    @DisplayName("Should process reopened issue event successfully")
    void shouldProcessReopenedIssueEvent() {
        // Given
        GitHubIssueEvent issueEvent = createValidIssueEvent();
        issueEvent.setAction("reopened");
        Bounty bounty = Bounty.builder()
                .issueId("42")
                .repositoryUrl("https://github.com/owner/test-repo")
                .platform("github")
                .amount(new java.math.BigDecimal("500.00"))
                .build();
        
        when(githubIssueScannerService.processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(bounty);
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertTrue(result);
        verify(githubIssueScannerService, times(1)).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return false for null issue event")
    void shouldReturnFalseForNullIssueEvent() {
        // When
        boolean result = webhookService.processIssueEvent(null);
        
        // Then
        assertFalse(result);
        verify(githubIssueScannerService, never()).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return false for issue event without issue")
    void shouldReturnFalseForIssueEventWithoutIssue() {
        // Given
        GitHubIssueEvent issueEvent = new GitHubIssueEvent();
        issueEvent.setIssue(null);
        issueEvent.setRepository(new GitHubIssueEvent.Repository());
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertFalse(result);
        verify(githubIssueScannerService, never()).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should return false for issue event without repository")
    void shouldReturnFalseForIssueEventWithoutRepository() {
        // Given
        GitHubIssueEvent issueEvent = new GitHubIssueEvent();
        issueEvent.setIssue(new GitHubIssueEvent.Issue());
        issueEvent.setRepository(null);
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertFalse(result);
        verify(githubIssueScannerService, never()).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip pull request events")
    void shouldSkipPullRequestEvents() {
        // Given
        GitHubIssueEvent issueEvent = createValidIssueEvent();
        GitHubIssueEvent.PullRequest pullRequest = new GitHubIssueEvent.PullRequest();
        pullRequest.setUrl("https://api.github.com/repos/owner/test-repo/pulls/42");
        issueEvent.getIssue().setPullRequest(pullRequest);
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertTrue(result); // Returns true but doesn't process
        verify(githubIssueScannerService, never()).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip closed issue events")
    void shouldSkipClosedIssueEvents() {
        // Given
        GitHubIssueEvent issueEvent = createValidIssueEvent();
        issueEvent.setAction("closed");
        issueEvent.getIssue().setState("closed");
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertTrue(result); // Returns true but doesn't process
        verify(githubIssueScannerService, never()).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip non-opened/reopened actions")
    void shouldSkipNonOpenedReopenedActions() {
        // Given
        GitHubIssueEvent issueEvent = createValidIssueEvent();
        issueEvent.setAction("edited");
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertTrue(result); // Returns true but doesn't process
        verify(githubIssueScannerService, never()).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle issue with no bounty found")
    void shouldHandleIssueWithNoBountyFound() {
        // Given
        GitHubIssueEvent issueEvent = createValidIssueEvent();
        when(githubIssueScannerService.processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null); // No bounty found
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertTrue(result); // Returns true even if no bounty found
        verify(githubIssueScannerService, times(1)).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle exception during issue processing")
    void shouldHandleExceptionDuringIssueProcessing() {
        // Given
        GitHubIssueEvent issueEvent = createValidIssueEvent();
        when(githubIssueScannerService.processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Processing error"));
        
        // When
        boolean result = webhookService.processIssueEvent(issueEvent);
        
        // Then
        assertFalse(result);
        verify(githubIssueScannerService, times(1)).processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should extract repository URL correctly")
    void shouldExtractRepositoryUrlCorrectly() {
        // Given
        GitHubIssueEvent issueEvent = createValidIssueEvent();
        when(githubIssueScannerService.processIssueFromWebhook(
                anyString(), anyString(), anyString(), anyString()))
                .thenReturn(null);
        
        // When
        webhookService.processIssueEvent(issueEvent);
        
        // Then
        verify(githubIssueScannerService).processIssueFromWebhook(
                eq("https://github.com/owner/test-repo"),
                anyString(), anyString(), anyString());
    }
}

