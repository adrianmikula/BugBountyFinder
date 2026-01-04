package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.domain.CVECatalog;
import com.bugbounty.cve.entity.CVECatalogEntity;
import com.bugbounty.cve.mapper.CVECatalogMapper;
import com.bugbounty.cve.repository.CVECatalogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CVECatalogService Tests")
class CVECatalogServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private AssistantMessage assistantMessage;

    @Mock
    private CVECatalogRepository catalogRepository;

    @Mock
    private CVECatalogMapper catalogMapper;

    @Mock
    private LanguageMappingService languageMappingService;

    @InjectMocks
    private CVECatalogService catalogService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Use reflection to set the objectMapper field
        try {
            java.lang.reflect.Field field = CVECatalogService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(catalogService, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set objectMapper", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void mockChatResponse(String jsonContent) {
        try {
            lenient().when(assistantMessage.getContent()).thenReturn(jsonContent);
            
            java.lang.reflect.Method getResultMethod = ChatResponse.class.getMethod("getResult");
            Class<?> resultType = getResultMethod.getReturnType();
            
            Object resultMock = mock(resultType, (org.mockito.stubbing.Answer<Object>) invocation -> {
                java.lang.reflect.Method method = invocation.getMethod();
                if ("getOutput".equals(method.getName())) {
                    return assistantMessage;
                }
                return null;
            });
            
            lenient().doReturn(resultMock).when(chatResponse).getResult();
            lenient().when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock ChatResponse", e);
        }
    }

    @Test
    @DisplayName("Should create catalog entry for CVE and language")
    void shouldCreateCatalogEntry() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .description("Test CVE description")
                .severity("HIGH")
                .cvssScore(8.5)
                .affectedLanguages(List.of("Java"))
                .affectedProducts(List.of("Spring Framework"))
                .build();

        when(languageMappingService.extractLanguages(any(), any()))
                .thenReturn(Set.of("Java"));

        when(catalogRepository.findByCveIdAndLanguage("CVE-2024-1234", "Java"))
                .thenReturn(Optional.empty());

        String jsonResponse = """
                {
                  "summary": "Vulnerability in Spring Framework",
                  "codeExample": "vulnerable code example",
                  "vulnerablePattern": "pattern description",
                  "fixedPattern": "fixed pattern",
                  "fixedCodeExample": "fixed code"
                }
                """;
        mockChatResponse(jsonResponse);

        CVECatalogEntity savedEntity = CVECatalogEntity.builder()
                .cveId("CVE-2024-1234")
                .language("Java")
                .summary("Vulnerability in Spring Framework")
                .build();

        when(catalogRepository.save(any(CVECatalogEntity.class))).thenReturn(savedEntity);
        when(catalogMapper.toDomain(any(CVECatalogEntity.class))).thenReturn(
                CVECatalog.builder()
                        .cveId("CVE-2024-1234")
                        .language("Java")
                        .summary("Vulnerability in Spring Framework")
                        .build()
        );

        // When
        Flux<CVECatalog> result = catalogService.processCVEForCatalog(cve);

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .expectComplete()
                .verify(java.time.Duration.ofSeconds(5));

        verify(catalogRepository, times(1)).save(any(CVECatalogEntity.class));
    }

    @Test
    @DisplayName("Should skip if catalog entry already exists")
    void shouldSkipIfCatalogEntryExists() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .affectedLanguages(List.of("Java"))
                .build();

        when(languageMappingService.extractLanguages(any(), any()))
                .thenReturn(Set.of("Java"));

        when(catalogRepository.findByCveIdAndLanguage("CVE-2024-1234", "Java"))
                .thenReturn(Optional.of(CVECatalogEntity.builder().build()));

        // When
        Flux<CVECatalog> result = catalogService.processCVEForCatalog(cve);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(catalogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return empty if no languages extracted")
    void shouldReturnEmptyIfNoLanguages() {
        // Given
        CVE cve = CVE.builder()
                .cveId("CVE-2024-1234")
                .build();

        when(languageMappingService.extractLanguages(any(), any()))
                .thenReturn(Set.of());

        // When
        Flux<CVECatalog> result = catalogService.processCVEForCatalog(cve);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(catalogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get catalog entries by language")
    void shouldGetCatalogEntriesByLanguage() {
        // Given
        CVECatalogEntity entity = CVECatalogEntity.builder()
                .cveId("CVE-2024-1234")
                .language("Java")
                .summary("Test summary")
                .build();

        when(catalogRepository.findByLanguage("Java"))
                .thenReturn(List.of(entity));
        when(catalogMapper.toDomain(entity))
                .thenReturn(CVECatalog.builder()
                        .cveId("CVE-2024-1234")
                        .language("Java")
                        .summary("Test summary")
                        .build());

        // When
        List<CVECatalog> result = catalogService.getCatalogEntriesByLanguage("Java");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CVE-2024-1234", result.get(0).getCveId());
    }
}

