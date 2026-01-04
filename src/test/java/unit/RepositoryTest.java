package com.bugbounty.repository.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Repository Domain Model Tests")
class RepositoryTest {

    @Test
    @DisplayName("Should create a valid repository with all required fields")
    void shouldCreateValidRepository() {
        // Given
        String url = "https://github.com/owner/repo";
        String owner = "owner";
        String name = "repo";
        String defaultBranch = "main";
        String language = "Java";

        // When
        Repository repository = Repository.builder()
                .id(UUID.randomUUID())
                .url(url)
                .owner(owner)
                .name(name)
                .defaultBranch(defaultBranch)
                .language(language)
                .build();

        // Then
        assertNotNull(repository.getId());
        assertEquals(url, repository.getUrl());
        assertEquals(owner, repository.getOwner());
        assertEquals(name, repository.getName());
        assertEquals(defaultBranch, repository.getDefaultBranch());
        assertEquals(language, repository.getLanguage());
    }

    @Test
    @DisplayName("Should extract owner and name from GitHub URL")
    void shouldExtractOwnerAndNameFromUrl() {
        // Given
        String url = "https://github.com/spring-projects/spring-boot";

        // When
        Repository repository = Repository.builder()
                .url(url)
                .build();

        // Then
        assertEquals("spring-projects", repository.getOwner());
        assertEquals("spring-boot", repository.getName());
    }

    @Test
    @DisplayName("Should handle different GitHub URL formats")
    void shouldHandleDifferentUrlFormats() {
        // Given
        String httpsUrl = "https://github.com/owner/repo";
        String sshUrl = "git@github.com:owner/repo.git";
        String httpUrl = "http://github.com/owner/repo";

        // When
        Repository repo1 = Repository.builder().url(httpsUrl).build();
        Repository repo2 = Repository.builder().url(sshUrl).build();
        Repository repo3 = Repository.builder().url(httpUrl).build();

        // Then
        assertEquals("owner", repo1.getOwner());
        assertEquals("repo", repo1.getName());
        assertEquals("owner", repo2.getOwner());
        assertEquals("repo", repo2.getName());
        assertEquals("owner", repo3.getOwner());
        assertEquals("repo", repo3.getName());
    }

    @Test
    @DisplayName("Should check if repository is cloned")
    void shouldCheckIfRepositoryIsCloned() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .localPath("/repos/owner/repo")
                .build();

        // Then
        assertTrue(repository.isCloned());
    }

    @Test
    @DisplayName("Should mark repository as cloned")
    void shouldMarkRepositoryAsCloned() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .build();

        // When
        repository.markAsCloned("/repos/owner/repo");

        // Then
        assertTrue(repository.isCloned());
        assertEquals("/repos/owner/repo", repository.getLocalPath());
    }

    @Test
    @DisplayName("Should get full repository path")
    void shouldGetFullRepositoryPath() {
        // Given
        Repository repository = Repository.builder()
                .owner("owner")
                .name("repo")
                .build();

        // Then
        assertEquals("owner/repo", repository.getFullPath());
    }

    @Test
    @DisplayName("Should return null for full path when owner is null")
    void shouldReturnNullForFullPathWhenOwnerIsNull() {
        // Given
        Repository repository = Repository.builder()
                .name("repo")
                .build();

        // Then
        assertNull(repository.getFullPath());
    }

    @Test
    @DisplayName("Should return null for full path when name is null")
    void shouldReturnNullForFullPathWhenNameIsNull() {
        // Given
        Repository repository = Repository.builder()
                .owner("owner")
                .build();

        // Then
        assertNull(repository.getFullPath());
    }

    @Test
    @DisplayName("Should extract owner and name from URL when not explicitly set")
    void shouldExtractOwnerAndNameFromUrlWhenNotSet() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/test-owner/test-repo")
                .build();

        // When
        String owner = repository.getOwner();
        String name = repository.getName();

        // Then
        assertEquals("test-owner", owner);
        assertEquals("test-repo", name);
    }

    @Test
    @DisplayName("Should return null owner when URL is null")
    void shouldReturnNullOwnerWhenUrlIsNull() {
        // Given
        Repository repository = Repository.builder()
                .url(null)
                .build();

        // Then
        assertNull(repository.getOwner());
    }

    @Test
    @DisplayName("Should return null name when URL is null")
    void shouldReturnNullNameWhenUrlIsNull() {
        // Given
        Repository repository = Repository.builder()
                .url(null)
                .build();

        // Then
        assertNull(repository.getName());
    }

    @Test
    @DisplayName("Should return false when local path is null")
    void shouldReturnFalseWhenLocalPathIsNull() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .localPath(null)
                .build();

        // Then
        assertFalse(repository.isCloned());
    }

    @Test
    @DisplayName("Should return false when local path is empty")
    void shouldReturnFalseWhenLocalPathIsEmpty() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .localPath("")
                .build();

        // Then
        assertFalse(repository.isCloned());
    }

    @Test
    @DisplayName("Should handle URL with trailing slash")
    void shouldHandleUrlWithTrailingSlash() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo/")
                .build();

        // Then
        assertEquals("owner", repository.getOwner());
        assertEquals("repo", repository.getName());
    }

    @Test
    @DisplayName("Should handle URL with .git extension")
    void shouldHandleUrlWithGitExtension() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo.git")
                .build();

        // Then
        assertEquals("owner", repository.getOwner());
        assertEquals("repo", repository.getName());
    }

    @Test
    @DisplayName("Should use explicitly set owner and name instead of extracting from URL")
    void shouldUseExplicitlySetOwnerAndName() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/wrong-owner/wrong-repo")
                .owner("correct-owner")
                .name("correct-repo")
                .build();

        // Then
        assertEquals("correct-owner", repository.getOwner());
        assertEquals("correct-repo", repository.getName());
    }

    @Test
    @DisplayName("Should generate UUID when id is not provided")
    void shouldGenerateUuidWhenIdNotProvided() {
        // When
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .build();

        // Then
        assertNotNull(repository.getId());
    }
}

