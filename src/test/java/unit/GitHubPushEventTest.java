package com.bugbounty.webhook.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitHubPushEvent DTO Tests")
class GitHubPushEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should extract branch name from ref")
    void shouldExtractBranchName() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/heads/main");
        
        // When
        String branchName = pushEvent.getBranchName();
        
        // Then
        assertEquals("main", branchName);
    }

    @Test
    @DisplayName("Should extract feature branch name")
    void shouldExtractFeatureBranchName() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/heads/feature/new-feature");
        
        // When
        String branchName = pushEvent.getBranchName();
        
        // Then
        assertEquals("feature/new-feature", branchName);
    }

    @Test
    @DisplayName("Should return null for invalid ref format")
    void shouldReturnNullForInvalidRefFormat() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("invalid-ref");
        
        // When
        String branchName = pushEvent.getBranchName();
        
        // Then
        assertNull(branchName);
    }

    @Test
    @DisplayName("Should return null for null ref")
    void shouldReturnNullForNullRef() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef(null);
        
        // When
        String branchName = pushEvent.getBranchName();
        
        // Then
        assertNull(branchName);
    }

    @Test
    @DisplayName("Should return null for tag ref")
    void shouldReturnNullForTagRef() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/tags/v1.0.0");
        
        // When
        String branchName = pushEvent.getBranchName();
        
        // Then
        assertNull(branchName);
    }

    @Test
    @DisplayName("Should check if push is to default branch")
    void shouldCheckIfPushIsToDefaultBranch() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/heads/main");
        
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setDefaultBranch("main");
        pushEvent.setRepository(repo);
        
        // When
        boolean isDefault = pushEvent.isDefaultBranch();
        
        // Then
        assertTrue(isDefault);
    }

    @Test
    @DisplayName("Should return false when push is not to default branch")
    void shouldReturnFalseWhenNotDefaultBranch() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/heads/feature-branch");
        
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setDefaultBranch("main");
        pushEvent.setRepository(repo);
        
        // When
        boolean isDefault = pushEvent.isDefaultBranch();
        
        // Then
        assertFalse(isDefault);
    }

    @Test
    @DisplayName("Should return false when repository is null")
    void shouldReturnFalseWhenRepositoryIsNull() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/heads/main");
        pushEvent.setRepository(null);
        
        // When
        boolean isDefault = pushEvent.isDefaultBranch();
        
        // Then
        assertFalse(isDefault);
    }

    @Test
    @DisplayName("Should return false when ref is null")
    void shouldReturnFalseWhenRefIsNull() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef(null);
        
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setDefaultBranch("main");
        pushEvent.setRepository(repo);
        
        // When
        boolean isDefault = pushEvent.isDefaultBranch();
        
        // Then
        assertFalse(isDefault);
    }

    @Test
    @DisplayName("Should return false when default branch is null")
    void shouldReturnFalseWhenDefaultBranchIsNull() {
        // Given
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/heads/main");
        
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setDefaultBranch(null);
        pushEvent.setRepository(repo);
        
        // When
        boolean isDefault = pushEvent.isDefaultBranch();
        
        // Then
        assertFalse(isDefault);
    }

    @Test
    @DisplayName("Should parse JSON payload correctly")
    void shouldParseJsonPayload() throws Exception {
        // Given
        String jsonPayload = """
                {
                  "ref": "refs/heads/main",
                  "repository": {
                    "id": 123456,
                    "name": "test-repo",
                    "full_name": "owner/test-repo",
                    "clone_url": "https://github.com/owner/test-repo.git",
                    "html_url": "https://github.com/owner/test-repo",
                    "default_branch": "main",
                    "private": false
                  },
                  "commits": [
                    {
                      "id": "abc123",
                      "message": "Fix bug",
                      "timestamp": "2024-01-01T12:00:00Z",
                      "added": ["file1.java"],
                      "modified": ["file2.java"],
                      "removed": []
                    }
                  ],
                  "pusher": {
                    "name": "testuser",
                    "email": "test@example.com"
                  }
                }
                """;
        
        // When
        GitHubPushEvent pushEvent = objectMapper.readValue(jsonPayload, GitHubPushEvent.class);
        
        // Then
        assertNotNull(pushEvent);
        assertEquals("refs/heads/main", pushEvent.getRef());
        assertNotNull(pushEvent.getRepository());
        assertEquals("owner/test-repo", pushEvent.getRepository().getFullName());
        assertEquals("main", pushEvent.getBranchName());
        assertTrue(pushEvent.isDefaultBranch());
        assertNotNull(pushEvent.getCommits());
        assertEquals(1, pushEvent.getCommits().size());
        assertEquals("abc123", pushEvent.getCommits().get(0).getId());
    }

    @Test
    @DisplayName("Should ignore unknown JSON properties")
    void shouldIgnoreUnknownJsonProperties() throws Exception {
        // Given
        String jsonPayload = """
                {
                  "ref": "refs/heads/main",
                  "repository": {
                    "name": "test-repo",
                    "clone_url": "https://github.com/owner/test-repo.git"
                  },
                  "unknown_field": "should be ignored",
                  "commits": []
                }
                """;
        
        // When
        GitHubPushEvent pushEvent = objectMapper.readValue(jsonPayload, GitHubPushEvent.class);
        
        // Then
        assertNotNull(pushEvent);
        assertEquals("refs/heads/main", pushEvent.getRef());
        assertNotNull(pushEvent.getRepository());
    }

    @Test
    @DisplayName("Should handle empty commits list")
    void shouldHandleEmptyCommitsList() throws Exception {
        // Given
        String jsonPayload = """
                {
                  "ref": "refs/heads/main",
                  "repository": {
                    "name": "test-repo",
                    "clone_url": "https://github.com/owner/test-repo.git"
                  },
                  "commits": []
                }
                """;
        
        // When
        GitHubPushEvent pushEvent = objectMapper.readValue(jsonPayload, GitHubPushEvent.class);
        
        // Then
        assertNotNull(pushEvent);
        assertNotNull(pushEvent.getCommits());
        assertTrue(pushEvent.getCommits().isEmpty());
    }

    @Test
    @DisplayName("Should handle commit with all fields")
    void shouldHandleCommitWithAllFields() {
        // Given
        GitHubPushEvent.Commit commit = new GitHubPushEvent.Commit();
        commit.setId("abc123def456");
        commit.setMessage("Fix critical bug");
        commit.setTimestamp("2024-01-01T12:00:00Z");
        commit.setAdded(Arrays.asList("new-file.java"));
        commit.setModified(Arrays.asList("modified-file.java"));
        commit.setRemoved(Arrays.asList("deleted-file.java"));
        commit.setUrl("https://github.com/owner/repo/commit/abc123");
        
        GitHubPushEvent.Author author = new GitHubPushEvent.Author();
        author.setName("John Doe");
        author.setEmail("john@example.com");
        commit.setAuthor(author);
        
        // When & Then
        assertNotNull(commit);
        assertEquals("abc123def456", commit.getId());
        assertEquals("Fix critical bug", commit.getMessage());
        assertEquals(1, commit.getAdded().size());
        assertEquals(1, commit.getModified().size());
        assertEquals(1, commit.getRemoved().size());
        assertNotNull(commit.getAuthor());
        assertEquals("John Doe", commit.getAuthor().getName());
    }

    @Test
    @DisplayName("Should handle repository with all fields")
    void shouldHandleRepositoryWithAllFields() {
        // Given
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setId(123456L);
        repo.setName("test-repo");
        repo.setFullName("owner/test-repo");
        repo.setCloneUrl("https://github.com/owner/test-repo.git");
        repo.setHtmlUrl("https://github.com/owner/test-repo");
        repo.setDefaultBranch("main");
        repo.setIsPrivate(false);
        
        // When & Then
        assertNotNull(repo);
        assertEquals(123456L, repo.getId());
        assertEquals("test-repo", repo.getName());
        assertEquals("owner/test-repo", repo.getFullName());
        assertEquals("main", repo.getDefaultBranch());
        assertFalse(repo.getIsPrivate());
    }
}

