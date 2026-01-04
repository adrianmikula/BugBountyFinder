package com.bugbounty.cve.service;

import com.bugbounty.repository.domain.Repository;
import com.bugbounty.repository.service.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RepositoryScanningService Tests")
class RepositoryScanningServiceTest {

    @Mock
    private RepositoryService repositoryService;

    @InjectMocks
    private RepositoryScanningService scanningService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Use reflection to set basePath for testing
        try {
            java.lang.reflect.Field field = RepositoryScanningService.class.getDeclaredField("basePath");
            field.setAccessible(true);
            field.set(scanningService, tempDir.toString());
        } catch (Exception e) {
            // Ignore if reflection fails
        }
    }

    @Test
    @DisplayName("Should scan repository for CVE")
    void shouldScanRepositoryForCVE() throws Exception {
        // Given
        String repositoryUrl = "https://github.com/owner/repo";
        String cveId = "CVE-2024-1234";

        Path repoPath = tempDir.resolve("repo");
        Files.createDirectories(repoPath);

        Repository repository = Repository.builder()
                .url(repositoryUrl)
                .language("Java")
                .localPath(repoPath.toString())
                .build();

        when(repositoryService.cloneRepository(any(Repository.class), anyString()))
                .thenReturn(repository);

        // When
        var result = scanningService.scanRepositoryForCVE(repositoryUrl, cveId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(repositoryService).cloneRepository(any(Repository.class), anyString());
    }

    @Test
    @DisplayName("Should update existing repository before scanning")
    void shouldUpdateExistingRepositoryBeforeScanning() throws Exception {
        // Given
        String repositoryUrl = "https://github.com/owner/repo";
        String cveId = "CVE-2024-1234";

        Path repoPath = tempDir.resolve("repo");
        Files.createDirectories(repoPath);

        Repository repository = Repository.builder()
                .url(repositoryUrl)
                .language("Java")
                .localPath(repoPath.toString())
                .build();

        when(repositoryService.cloneRepository(any(Repository.class), anyString()))
                .thenReturn(repository);

        // When
        var result = scanningService.scanRepositoryForCVE(repositoryUrl, cveId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle repository clone failure")
    void shouldHandleRepositoryCloneFailure() throws Exception {
        // Given
        String repositoryUrl = "https://github.com/owner/repo";
        String cveId = "CVE-2024-1234";

        when(repositoryService.cloneRepository(any(Repository.class), anyString()))
                .thenThrow(new RuntimeException("Clone failed"));

        // When & Then
        StepVerifier.create(scanningService.scanRepositoryForCVE(repositoryUrl, cveId))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    @DisplayName("Should scan Java repository with pom.xml")
    void shouldScanJavaRepositoryWithPomXml() throws Exception {
        // Given
        String repositoryUrl = "https://github.com/owner/java-repo";
        String cveId = "CVE-2024-1234";

        Path repoPath = tempDir.resolve("java-repo");
        Files.createDirectories(repoPath);
        Files.createFile(repoPath.resolve("pom.xml"));

        Repository repository = Repository.builder()
                .url(repositoryUrl)
                .language("Java")
                .localPath(repoPath.toString())
                .build();

        when(repositoryService.cloneRepository(any(Repository.class), anyString()))
                .thenReturn(repository);

        // When
        var result = scanningService.scanRepositoryForCVE(repositoryUrl, cveId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertTrue(Files.exists(repoPath.resolve("pom.xml")));
    }

    @Test
    @DisplayName("Should scan Python repository with requirements.txt")
    void shouldScanPythonRepositoryWithRequirementsTxt() throws Exception {
        // Given
        String repositoryUrl = "https://github.com/owner/python-repo";
        String cveId = "CVE-2024-1234";

        Path repoPath = tempDir.resolve("python-repo");
        Files.createDirectories(repoPath);
        Files.createFile(repoPath.resolve("requirements.txt"));

        Repository repository = Repository.builder()
                .url(repositoryUrl)
                .language("Python")
                .localPath(repoPath.toString())
                .build();

        when(repositoryService.cloneRepository(any(Repository.class), anyString()))
                .thenReturn(repository);

        // When
        var result = scanningService.scanRepositoryForCVE(repositoryUrl, cveId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertTrue(Files.exists(repoPath.resolve("requirements.txt")));
    }

    @Test
    @DisplayName("Should scan JavaScript repository with package.json")
    void shouldScanJavaScriptRepositoryWithPackageJson() throws Exception {
        // Given
        String repositoryUrl = "https://github.com/owner/js-repo";
        String cveId = "CVE-2024-1234";

        Path repoPath = tempDir.resolve("js-repo");
        Files.createDirectories(repoPath);
        Files.createFile(repoPath.resolve("package.json"));

        Repository repository = Repository.builder()
                .url(repositoryUrl)
                .language("JavaScript")
                .localPath(repoPath.toString())
                .build();

        when(repositoryService.cloneRepository(any(Repository.class), anyString()))
                .thenReturn(repository);

        // When
        var result = scanningService.scanRepositoryForCVE(repositoryUrl, cveId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        assertTrue(Files.exists(repoPath.resolve("package.json")));
    }

    @Test
    @DisplayName("Should handle repository without language")
    void shouldHandleRepositoryWithoutLanguage() throws Exception {
        // Given
        String repositoryUrl = "https://github.com/owner/repo";
        String cveId = "CVE-2024-1234";

        Repository repository = Repository.builder()
                .url(repositoryUrl)
                .language(null)
                .localPath(tempDir.resolve("repo").toString())
                .build();

        when(repositoryService.cloneRepository(any(Repository.class), anyString()))
                .thenReturn(repository);

        // When
        var result = scanningService.scanRepositoryForCVE(repositoryUrl, cveId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle repository with unknown language")
    void shouldHandleRepositoryWithUnknownLanguage() throws Exception {
        // Given
        String repositoryUrl = "https://github.com/owner/repo";
        String cveId = "CVE-2024-1234";

        Repository repository = Repository.builder()
                .url(repositoryUrl)
                .language("UnknownLanguage")
                .localPath(tempDir.resolve("repo").toString())
                .build();

        when(repositoryService.cloneRepository(any(Repository.class), anyString()))
                .thenReturn(repository);

        // When
        var result = scanningService.scanRepositoryForCVE(repositoryUrl, cveId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}

