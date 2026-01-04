package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.entity.CVEEntity;
import com.bugbounty.cve.mapper.CVEMapper;
import com.bugbounty.cve.repository.CVERepository;
import com.bugbounty.repository.entity.RepositoryEntity;
import com.bugbounty.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CVEMonitoringService Tests")
class CVEMonitoringServiceTest {

    @Mock
    private NvdApiClient nvdApiClient;

    @Mock
    private CVERepository cveRepository;

    @Mock
    private CVEMapper cveMapper;

    @Mock
    private LanguageMappingService languageMappingService;

    @Mock
    private RepositoryScanningService repositoryScanningService;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private CVECatalogService cveCatalogService;

    @InjectMocks
    private CVEMonitoringService monitoringService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Should process new CVE and trigger repository scans")
    void shouldProcessNewCVEAndTriggerScans() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedProducts(Arrays.asList("Spring Framework"))
                .build();

        CVEEntity entity = CVEEntity.builder()
                .id(UUID.randomUUID())
                .cveId("CVE-2024-1234")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        RepositoryEntity repo1 = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .url("https://github.com/owner/repo1")
                .language("Java")
                .build();

        RepositoryEntity repo2 = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .url("https://github.com/owner/repo2")
                .language("Python")
                .build();

        when(cveRepository.existsByCveId("CVE-2024-1234")).thenReturn(false);
        when(cveMapper.toEntity(any(CVE.class))).thenReturn(entity);
        when(cveRepository.save(any(CVEEntity.class))).thenReturn(entity);
        when(languageMappingService.extractLanguages(any(), any())).thenReturn(
                java.util.Set.of("Java")
        );
        when(repositoryRepository.findAll()).thenReturn(Arrays.asList(repo1, repo2));
        when(languageMappingService.matchesLanguage("Java", java.util.Set.of("Java")))
                .thenReturn(true);
        when(languageMappingService.matchesLanguage("Python", java.util.Set.of("Java")))
                .thenReturn(false);
        when(repositoryScanningService.scanRepositoryForCVE(anyString(), anyString()))
                .thenReturn(reactor.core.publisher.Mono.empty());
        when(cveCatalogService.processCVEForCatalog(any(CVE.class)))
                .thenReturn(reactor.core.publisher.Flux.empty());

        // When
        var result = monitoringService.processNewCVE(cve)
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        verify(cveRepository).save(any(CVEEntity.class));
        verify(languageMappingService).extractLanguages(any(), any());
        verify(repositoryScanningService, times(1)).scanRepositoryForCVE(
                eq("https://github.com/owner/repo1"), eq("CVE-2024-1234")
        );
        verify(repositoryScanningService, never()).scanRepositoryForCVE(
                eq("https://github.com/owner/repo2"), anyString()
        );
    }

    @Test
    @DisplayName("Should skip CVE if it already exists")
    void shouldSkipCVEIfAlreadyExists() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        when(cveRepository.existsByCveId("CVE-2024-1234")).thenReturn(true);
        lenient().when(cveCatalogService.processCVEForCatalog(any(CVE.class)))
                .thenReturn(reactor.core.publisher.Flux.empty());

        // When
        var result = monitoringService.processNewCVE(cve)
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(cveRepository, never()).save(any());
        verify(repositoryScanningService, never()).scanRepositoryForCVE(anyString(), anyString());
    }

    @Test
    @DisplayName("Should skip repository scan when no languages extracted")
    void shouldSkipScanWhenNoLanguagesExtracted() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        CVEEntity entity = CVEEntity.builder()
                .id(UUID.randomUUID())
                .cveId("CVE-2024-1234")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        when(cveRepository.existsByCveId("CVE-2024-1234")).thenReturn(false);
        when(cveMapper.toEntity(any(CVE.class))).thenReturn(entity);
        when(cveRepository.save(any(CVEEntity.class))).thenReturn(entity);
        when(languageMappingService.extractLanguages(any(), any())).thenReturn(
                java.util.Set.of()
        );
        lenient().when(repositoryRepository.findAll()).thenReturn(Arrays.asList());
        lenient().when(cveCatalogService.processCVEForCatalog(any(CVE.class)))
                .thenReturn(reactor.core.publisher.Flux.empty());

        // When
        var result = monitoringService.processNewCVE(cve)
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        verify(cveRepository).save(any(CVEEntity.class));
        verify(repositoryScanningService, never()).scanRepositoryForCVE(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle webhook notification")
    void shouldHandleWebhookNotification() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-5678")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("WEBHOOK")
                .build();

        CVEEntity entity = CVEEntity.builder()
                .id(UUID.randomUUID())
                .cveId("CVE-2024-5678")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("WEBHOOK")
                .build();

        when(cveRepository.existsByCveId("CVE-2024-5678")).thenReturn(false);
        when(cveMapper.toEntity(any(CVE.class))).thenReturn(entity);
        when(cveRepository.save(any(CVEEntity.class))).thenReturn(entity);
        when(languageMappingService.extractLanguages(any(), any())).thenReturn(
                java.util.Set.of("Java")
        );
        lenient().when(repositoryRepository.findAll()).thenReturn(Arrays.asList());
        lenient().when(cveCatalogService.processCVEForCatalog(any(CVE.class)))
                .thenReturn(reactor.core.publisher.Flux.empty());

        // When
        monitoringService.handleCVEWebhook(cve);

        // Then
        verify(cveRepository).save(any(CVEEntity.class));
    }

    @Test
    @DisplayName("Should skip webhook if CVE already exists")
    void shouldSkipWebhookIfCVEAlreadyExists() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-5678")
                .severity("HIGH")
                .publishedDate(LocalDateTime.now())
                .source("WEBHOOK")
                .build();

        when(cveRepository.existsByCveId("CVE-2024-5678")).thenReturn(true);

        // When
        monitoringService.handleCVEWebhook(cve);

        // Then
        verify(cveRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should filter repositories by language")
    void shouldFilterRepositoriesByLanguage() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .affectedProducts(Arrays.asList("Spring Framework"))
                .build();

        CVEEntity entity = CVEEntity.builder()
                .id(UUID.randomUUID())
                .cveId("CVE-2024-1234")
                .severity("CRITICAL")
                .publishedDate(LocalDateTime.now())
                .source("NVD")
                .build();

        RepositoryEntity javaRepo = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .url("https://github.com/owner/java-repo")
                .language("Java")
                .build();

        RepositoryEntity pythonRepo = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .url("https://github.com/owner/python-repo")
                .language("Python")
                .build();

        RepositoryEntity jsRepo = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .url("https://github.com/owner/js-repo")
                .language("JavaScript")
                .build();

        when(cveRepository.existsByCveId("CVE-2024-1234")).thenReturn(false);
        when(cveMapper.toEntity(any(CVE.class))).thenReturn(entity);
        when(cveRepository.save(any(CVEEntity.class))).thenReturn(entity);
        when(languageMappingService.extractLanguages(any(), any())).thenReturn(
                java.util.Set.of("Java")
        );
        when(repositoryRepository.findAll()).thenReturn(
                Arrays.asList(javaRepo, pythonRepo, jsRepo)
        );
        when(languageMappingService.matchesLanguage(eq("Java"), any()))
                .thenReturn(true);
        when(languageMappingService.matchesLanguage(eq("Python"), any()))
                .thenReturn(false);
        when(languageMappingService.matchesLanguage(eq("JavaScript"), any()))
                .thenReturn(false);
        when(repositoryScanningService.scanRepositoryForCVE(anyString(), anyString()))
                .thenReturn(reactor.core.publisher.Mono.empty());
        when(cveCatalogService.processCVEForCatalog(any(CVE.class)))
                .thenReturn(reactor.core.publisher.Flux.empty());

        // When
        var result = monitoringService.processNewCVE(cve)
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        verify(repositoryScanningService, times(1)).scanRepositoryForCVE(
                eq("https://github.com/owner/java-repo"), eq("CVE-2024-1234")
        );
        verify(repositoryScanningService, never()).scanRepositoryForCVE(
                eq("https://github.com/owner/python-repo"), anyString()
        );
        verify(repositoryScanningService, never()).scanRepositoryForCVE(
                eq("https://github.com/owner/js-repo"), anyString()
        );
    }
}

