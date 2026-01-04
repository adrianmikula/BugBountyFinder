package com.bugbounty.component;

import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.service.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Component test for RepositoryService.
 * Tests Git operations in a real environment.
 */
@DisplayName("Repository Service Component Tests")
class RepositoryServiceComponentTest extends AbstractComponentTest {

    @Autowired
    private RepositoryService repositoryService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Setup before each test
    }

    @Test
    @DisplayName("Should clone a repository successfully")
    void shouldCloneRepository() throws Exception {
        // Given - Use a small public test repo
        Repository repository = Repository.builder()
                .url("https://github.com/octocat/Hello-World.git")
                .owner("octocat")
                .name("Hello-World")
                .build();

        String clonePath = tempDir.resolve("repos").toString();

        // When
        Repository cloned = repositoryService.cloneRepository(repository, clonePath);

        // Then
        assertNotNull(cloned);
        assertTrue(cloned.isCloned());
        assertNotNull(cloned.getLocalPath());
        assertThat(cloned.getLocalPath()).contains("Hello-World");
    }

    @Test
    @DisplayName("Should check if repository is cloned")
    void shouldCheckIfRepositoryIsCloned() {
        // Given
        Repository notCloned = Repository.builder()
                .url("https://github.com/test/repo")
                .build();

        Repository cloned = Repository.builder()
                .url("https://github.com/test/repo")
                .localPath("/some/path")
                .build();

        // When & Then
        assertFalse(repositoryService.isCloned(notCloned));
        assertTrue(repositoryService.isCloned(cloned));
    }

    @Test
    @DisplayName("Should extract owner and name from URL")
    void shouldExtractOwnerAndNameFromUrl() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/spring-projects/spring-boot")
                .build();

        // When & Then
        assertEquals("spring-projects", repository.getOwner());
        assertEquals("spring-boot", repository.getName());
        assertEquals("spring-projects/spring-boot", repository.getFullPath());
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
}

