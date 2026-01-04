package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.BugFinding;
import com.bugbounty.cve.entity.CVECatalogEntity;
import com.bugbounty.cve.entity.CodebaseIndexEntity;
import com.bugbounty.cve.mapper.BugFindingMapper;
import com.bugbounty.cve.repository.BugFindingRepository;
import com.bugbounty.cve.repository.CVECatalogRepository;
import com.bugbounty.repository.service.RepositoryService;
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
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CommitAnalysisService Tests")
class CommitAnalysisServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private AssistantMessage assistantMessage;

    @Mock
    private CVECatalogRepository catalogRepository;

    @Mock
    private CodebaseIndexService codebaseIndexService;

    @Mock
    private BugFindingRepository bugFindingRepository;

    @Mock
    private BugFindingMapper bugFindingMapper;

    @Mock
    private RepositoryService repositoryService;

    @InjectMocks
    private CommitAnalysisService commitAnalysisService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        try {
            java.lang.reflect.Field field = CommitAnalysisService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(commitAnalysisService, objectMapper);
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
    @DisplayName("Should return empty if no catalog entries found")
    void shouldReturnEmptyIfNoCatalogEntries() {
        // Given
        when(catalogRepository.findByLanguage("Java"))
                .thenReturn(List.of());

        // When
        var result = commitAnalysisService.analyzeCommit(
                "https://github.com/owner/repo",
                "abc123",
                "diff content",
                List.of("file.java"),
                "Java"
        );

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(chatClient, never()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should detect CVEs in commit")
    void shouldDetectCVEsInCommit() {
        // Given
        CVECatalogEntity catalogEntity = CVECatalogEntity.builder()
                .cveId("CVE-2024-1234")
                .language("Java")
                .summary("Test CVE")
                .vulnerablePattern("pattern")
                .fixedPattern("fixed")
                .build();

        when(catalogRepository.findByLanguage("Java"))
                .thenReturn(List.of(catalogEntity));

        CodebaseIndexEntity index = CodebaseIndexEntity.builder()
                .indexData("{}")
                .build();

        when(codebaseIndexService.getIndex("https://github.com/owner/repo", "Java"))
                .thenReturn(Optional.of(index));

        // Mock initial analysis - no CVEs detected
        String initialResponse = "[]";
        mockChatResponse(initialResponse);

        // When
        var result = commitAnalysisService.analyzeCommit(
                "https://github.com/owner/repo",
                "abc123",
                "diff content",
                List.of("file.java"),
                "Java"
        );

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(chatClient, atLeastOnce()).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should create bug finding when CVE detected")
    void shouldCreateBugFindingWhenCVEDetected() {
        // Given
        CVECatalogEntity catalogEntity = CVECatalogEntity.builder()
                .cveId("CVE-2024-1234")
                .language("Java")
                .summary("Test CVE")
                .vulnerablePattern("pattern")
                .fixedPattern("fixed")
                .build();

        when(catalogRepository.findByLanguage("Java"))
                .thenReturn(List.of(catalogEntity));

        CodebaseIndexEntity index = CodebaseIndexEntity.builder()
                .indexData("{}")
                .build();

        when(codebaseIndexService.getIndex("https://github.com/owner/repo", "Java"))
                .thenReturn(Optional.of(index));

        // Mock initial analysis - CVE detected
        String initialResponse = "[\"CVE-2024-1234\"]";
        mockChatResponse(initialResponse);

        // Mock individual CVE analysis
        String individualResponse = """
                {
                  "cveId": "CVE-2024-1234",
                  "present": true,
                  "confidence": 0.8,
                  "vulnerableCode": "vulnerable code",
                  "recommendedFix": "fix code",
                  "notes": "analysis notes"
                }
                """;
        mockChatResponse(individualResponse);

        com.bugbounty.cve.entity.BugFindingEntity savedEntity = 
                com.bugbounty.cve.entity.BugFindingEntity.builder()
                        .id(java.util.UUID.randomUUID())
                        .cveId("CVE-2024-1234")
                        .status(com.bugbounty.cve.entity.BugFindingEntity.BugFindingStatus.DETECTED)
                        .build();

        when(bugFindingRepository.save(any(com.bugbounty.cve.entity.BugFindingEntity.class)))
                .thenReturn(savedEntity);
        when(bugFindingMapper.toDomain(any(com.bugbounty.cve.entity.BugFindingEntity.class)))
                .thenReturn(BugFinding.builder()
                        .cveId("CVE-2024-1234")
                        .status(BugFinding.BugFindingStatus.DETECTED)
                        .presenceConfidence(0.8)
                        .build());

        // When
        var result = commitAnalysisService.analyzeCommit(
                "https://github.com/owner/repo",
                "abc123",
                "diff content",
                List.of("file.java"),
                "Java"
        );

        // Then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(bugFindingRepository, times(1)).save(any());
    }
}

