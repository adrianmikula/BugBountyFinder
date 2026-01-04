package com.bugbounty.cve.service;

import com.bugbounty.cve.domain.BugFinding;
import com.bugbounty.cve.entity.BugFindingEntity;
import com.bugbounty.cve.entity.CVECatalogEntity;
import com.bugbounty.cve.entity.CodebaseIndexEntity;
import com.bugbounty.cve.mapper.BugFindingMapper;
import com.bugbounty.cve.repository.BugFindingRepository;
import com.bugbounty.cve.repository.CVECatalogRepository;
import com.bugbounty.repository.repository.RepositoryRepository;
import com.bugbounty.repository.entity.RepositoryEntity;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CVEVerificationService Tests")
class CVEVerificationServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private AssistantMessage assistantMessage;

    @Mock
    private BugFindingRepository bugFindingRepository;

    @Mock
    private BugFindingMapper bugFindingMapper;

    @Mock
    private CVECatalogRepository catalogRepository;

    @Mock
    private CodebaseIndexService codebaseIndexService;

    @Mock
    private RepositoryRepository repositoryRepository;

    @InjectMocks
    private CVEVerificationService verificationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        try {
            java.lang.reflect.Field field = CVEVerificationService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(verificationService, objectMapper);
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
    @DisplayName("Should verify and process bug finding")
    void shouldVerifyAndProcessBugFinding() {
        // Given
        UUID findingId = UUID.randomUUID();
        
        BugFindingEntity entity = BugFindingEntity.builder()
                .id(findingId)
                .repositoryUrl("https://github.com/owner/repo")
                .commitId("abc123")
                .cveId("CVE-2024-1234")
                .status(BugFindingEntity.BugFindingStatus.DETECTED)
                .presenceConfidence(0.7)
                .commitDiff("diff")
                .affectedFiles("[\"file.java\"]")
                .build();

        when(bugFindingRepository.findById(findingId))
                .thenReturn(Optional.of(entity));
        when(bugFindingMapper.toDomain(entity))
                .thenReturn(BugFinding.builder()
                        .id(findingId)
                        .repositoryUrl("https://github.com/owner/repo")
                        .commitId("abc123")
                        .cveId("CVE-2024-1234")
                        .status(BugFinding.BugFindingStatus.DETECTED)
                        .presenceConfidence(0.7)
                        .commitDiff("diff")
                        .affectedFiles(List.of("file.java"))
                        .build());

        RepositoryEntity repoEntity = RepositoryEntity.builder()
                .language("Java")
                .build();
        when(repositoryRepository.findByUrl("https://github.com/owner/repo"))
                .thenReturn(Optional.of(repoEntity));

        CVECatalogEntity catalog = CVECatalogEntity.builder()
                .cveId("CVE-2024-1234")
                .language("Java")
                .summary("Test CVE")
                .vulnerablePattern("pattern")
                .fixedPattern("fixed")
                .build();
        when(catalogRepository.findByCveIdAndLanguage("CVE-2024-1234", "Java"))
                .thenReturn(Optional.of(catalog));

        CodebaseIndexEntity index = CodebaseIndexEntity.builder()
                .indexData("{}")
                .build();
        when(codebaseIndexService.getIndex("https://github.com/owner/repo", "Java"))
                .thenReturn(Optional.of(index));

        // Mock verification response
        String verificationResponse = """
                {
                  "present": true,
                  "confidence": 0.9,
                  "vulnerableCode": "vulnerable code",
                  "notes": "verified"
                }
                """;
        mockChatResponse(verificationResponse);

        // Mock fix generation response
        String fixResponse = """
                {
                  "fixCode": "fixed code",
                  "notes": "fix generated"
                }
                """;
        mockChatResponse(fixResponse);

        // Mock confirmation response
        String confirmationResponse = """
                {
                  "correct": true,
                  "confidence": 0.95,
                  "notes": "fix confirmed",
                  "suggestions": "none"
                }
                """;
        mockChatResponse(confirmationResponse);

        when(bugFindingRepository.save(any(BugFindingEntity.class)))
                .thenAnswer(invocation -> {
                    BugFindingEntity saved = invocation.getArgument(0);
                    // Update the entity to reflect saved state
                    return saved;
                });
        when(bugFindingMapper.toEntity(any(BugFinding.class)))
                .thenAnswer(invocation -> {
                    BugFinding finding = invocation.getArgument(0);
                    BugFindingEntity mappedEntity = BugFindingEntity.builder()
                            .id(finding.getId())
                            .cveId(finding.getCveId())
                            .status(BugFindingEntity.BugFindingStatus.valueOf(finding.getStatus().name()))
                            .presenceConfidence(finding.getPresenceConfidence())
                            .fixConfidence(finding.getFixConfidence())
                            .build();
                    return mappedEntity;
                });
        when(bugFindingMapper.toDomain(any(BugFindingEntity.class)))
                .thenAnswer(invocation -> {
                    BugFindingEntity e = invocation.getArgument(0);
                    return BugFinding.builder()
                            .id(e.getId())
                            .cveId(e.getCveId())
                            .status(BugFinding.BugFindingStatus.valueOf(e.getStatus().name()))
                            .presenceConfidence(e.getPresenceConfidence() != null ? e.getPresenceConfidence() : 0.0)
                            .fixConfidence(e.getFixConfidence() != null ? e.getFixConfidence() : 0.0)
                            .build();
                });

        // When
        var result = verificationService.verifyAndProcessBugFinding(findingId);

        // Then
        StepVerifier.create(result)
                .assertNext(finding -> {
                    assertNotNull(finding);
                    assertEquals(findingId, finding.getId());
                    assertNotNull(finding.getPresenceConfidence(), "Presence confidence should not be null");
                    assertTrue(finding.getPresenceConfidence() >= 0.7, 
                            "Presence confidence should be >= 0.7, but was: " + finding.getPresenceConfidence());
                })
                .expectComplete()
                .verify(java.time.Duration.ofSeconds(5));

        verify(bugFindingRepository, atLeastOnce()).save(any(BugFindingEntity.class));
    }

    @Test
    @DisplayName("Should handle low confidence and require human review")
    void shouldHandleLowConfidence() {
        // Given
        UUID findingId = UUID.randomUUID();
        
        BugFindingEntity entity = BugFindingEntity.builder()
                .id(findingId)
                .repositoryUrl("https://github.com/owner/repo")
                .commitId("abc123")
                .cveId("CVE-2024-1234")
                .status(BugFindingEntity.BugFindingStatus.DETECTED)
                .build();

        when(bugFindingRepository.findById(findingId))
                .thenReturn(Optional.of(entity));
        when(bugFindingMapper.toDomain(entity))
                .thenReturn(BugFinding.builder()
                        .id(findingId)
                        .repositoryUrl("https://github.com/owner/repo")
                        .cveId("CVE-2024-1234")
                        .status(BugFinding.BugFindingStatus.DETECTED)
                        .build());

        RepositoryEntity repoEntity = RepositoryEntity.builder()
                .language("Java")
                .build();
        when(repositoryRepository.findByUrl("https://github.com/owner/repo"))
                .thenReturn(Optional.of(repoEntity));

        CVECatalogEntity catalog = CVECatalogEntity.builder()
                .cveId("CVE-2024-1234")
                .language("Java")
                .summary("Test CVE")
                .build();
        when(catalogRepository.findByCveIdAndLanguage("CVE-2024-1234", "Java"))
                .thenReturn(Optional.of(catalog));

        CodebaseIndexEntity index = CodebaseIndexEntity.builder()
                .indexData("{}")
                .build();
        when(codebaseIndexService.getIndex("https://github.com/owner/repo", "Java"))
                .thenReturn(Optional.of(index));

        // Mock low confidence verification
        String verificationResponse = """
                {
                  "present": false,
                  "confidence": 0.3,
                  "notes": "uncertain"
                }
                """;
        mockChatResponse(verificationResponse);

        when(bugFindingRepository.save(any(BugFindingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(bugFindingMapper.toEntity(any(BugFinding.class)))
                .thenAnswer(invocation -> {
                    BugFinding finding = invocation.getArgument(0);
                    return BugFindingEntity.builder()
                            .id(finding.getId())
                            .cveId(finding.getCveId())
                            .status(BugFindingEntity.BugFindingStatus.valueOf(finding.getStatus().name()))
                            .requiresHumanReview(finding.getRequiresHumanReview())
                            .build();
                });
        when(bugFindingMapper.toDomain(any(BugFindingEntity.class)))
                .thenAnswer(invocation -> {
                    BugFindingEntity e = invocation.getArgument(0);
                    return BugFinding.builder()
                            .id(e.getId())
                            .cveId(e.getCveId())
                            .status(BugFinding.BugFindingStatus.valueOf(e.getStatus().name()))
                            .requiresHumanReview(e.getRequiresHumanReview())
                            .build();
                });

        // When
        var result = verificationService.verifyAndProcessBugFinding(findingId);

        // Then
        StepVerifier.create(result)
                .assertNext(finding -> {
                    assertNotNull(finding);
                    assertTrue(finding.getRequiresHumanReview());
                    assertEquals(BugFinding.BugFindingStatus.HUMAN_REVIEW, finding.getStatus());
                })
                .verifyComplete();
    }
}

