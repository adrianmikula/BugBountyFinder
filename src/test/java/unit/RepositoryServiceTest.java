package com.bugbounty.repository.service;

import com.bugbounty.repository.domain.Repository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepositoryService Tests")
class RepositoryServiceTest {

    @Mock
    private GitOperations gitOperations;

    @InjectMocks
    private RepositoryService repositoryService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Setup test configuration
    }

    @Test
    @DisplayName("Should clone repository successfully")
    void shouldCloneRepository() throws Exception {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .owner("owner")
                .name("repo")
                .build();

        String expectedPath = tempDir.resolve("owner/repo").toString();
        when(gitOperations.cloneRepository(anyString(), anyString()))
                .thenReturn(mock(Git.class));

        // When
        Repository cloned = repositoryService.cloneRepository(repository, tempDir.toString());

        // Then
        assertNotNull(cloned);
        assertTrue(cloned.isCloned());
        verify(gitOperations, times(1)).cloneRepository(
                eq(repository.getUrl()),
                anyString()
        );
    }

    @Test
    @DisplayName("Should handle clone failure gracefully")
    void shouldHandleCloneFailure() throws Exception {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .build();

        when(gitOperations.cloneRepository(anyString(), anyString()))
                .thenThrow(new GitAPIException("Clone failed") {});

        // When & Then
        assertThrows(Exception.class, () -> {
            repositoryService.cloneRepository(repository, tempDir.toString());
        });
    }

    @Test
    @DisplayName("Should check if repository is already cloned")
    void shouldCheckIfRepositoryIsCloned() {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .localPath("/repos/owner/repo")
                .build();

        // When
        boolean isCloned = repositoryService.isCloned(repository);

        // Then
        assertTrue(isCloned);
    }

    @Test
    @DisplayName("Should update repository from remote")
    void shouldUpdateRepository() throws Exception {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .localPath("/repos/owner/repo")
                .build();

        Git gitMock = mock(Git.class);
        when(gitOperations.openRepository(anyString())).thenReturn(gitMock);
        when(gitOperations.pull(any(Git.class))).thenReturn(mock(org.eclipse.jgit.api.PullResult.class));

        // When
        repositoryService.updateRepository(repository);

        // Then
        verify(gitOperations, times(1)).openRepository(repository.getLocalPath());
        verify(gitOperations, times(1)).pull(gitMock);
    }

    @Test
    @DisplayName("Should get repository file content")
    void shouldGetFileContent() throws Exception {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .localPath("/repos/owner/repo")
                .build();

        String expectedContent = "public class Test {}";
        when(gitOperations.readFile(anyString(), anyString()))
                .thenReturn(expectedContent);

        // When
        String content = repositoryService.getFileContent(repository, "src/main/java/Test.java");

        // Then
        assertEquals(expectedContent, content);
        verify(gitOperations, times(1)).readFile(
                repository.getLocalPath(),
                "src/main/java/Test.java"
        );
    }

    @Test
    @DisplayName("Should get repository file list")
    void shouldGetFileList() throws Exception {
        // Given
        Repository repository = Repository.builder()
                .url("https://github.com/owner/repo")
                .localPath("/repos/owner/repo")
                .build();

        String[] expectedFiles = {"file1.java", "file2.java"};
        when(gitOperations.listFiles(anyString(), anyString()))
                .thenReturn(expectedFiles);

        // When
        String[] files = repositoryService.getFiles(repository, "src/main/java");

        // Then
        assertArrayEquals(expectedFiles, files);
        verify(gitOperations, times(1)).listFiles(
                repository.getLocalPath(),
                "src/main/java"
        );
    }
}

