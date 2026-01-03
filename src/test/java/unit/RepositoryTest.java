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
}

