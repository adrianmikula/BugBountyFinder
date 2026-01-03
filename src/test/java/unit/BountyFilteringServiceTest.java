package com.bugbounty.bounty.triage;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BountyFilteringService Tests")
class BountyFilteringServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private AssistantMessage assistantMessage;

    private ObjectMapper objectMapper;

    private BountyFilteringService filteringService;

    @BeforeEach
    void setUp() {
        // Use real ObjectMapper for JSON parsing
        objectMapper = new ObjectMapper();
        filteringService = new BountyFilteringService(chatClient, objectMapper);
    }

    /**
     * Helper method to mock the ChatResponse result chain.
     * Uses reflection to work around the inaccessible Generation type.
     */
    @SuppressWarnings({"unchecked"})
    private void mockChatResponseResult(String jsonContent) {
        try {
            java.lang.reflect.Method getResultMethod = ChatResponse.class.getMethod("getResult");
            Class<?> resultType = getResultMethod.getReturnType();
            
            // Set up assistantMessage content with lenient stubbing
            lenient().when(assistantMessage.getContent()).thenReturn(jsonContent);
            
            // Create a mock for the result with Answer to intercept method calls
            Object resultMock = mock(resultType, (org.mockito.stubbing.Answer<Object>) invocation -> {
                java.lang.reflect.Method method = invocation.getMethod();
                if ("getOutput".equals(method.getName())) {
                    return assistantMessage;
                }
                // For other methods, return null (Mockito default)
                return null;
            });
            
            // Use lenient doReturn to set up getResult() to return our mock
            lenient().doReturn(resultMock).when(chatResponse).getResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to mock ChatResponse result: " + e.getMessage(), e);
        }
    }

    @Test
    @DisplayName("Should accept high-value, fixable bounty")
    void shouldAcceptHighValueFixableBounty() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("200.00"))
                .title("Fix simple typo in README")
                .description("There's a typo in the README file on line 42")
                .status(BountyStatus.OPEN)
                .build();

        // Mock the result chain: chatResponse.getResult().getOutput() returns assistantMessage
        String jsonResponse = """
                {
                  "shouldProcess": true,
                  "confidence": 0.9,
                  "estimatedTimeMinutes": 15,
                  "reason": "Simple typo fix, low complexity, high confidence"
                }
                """;
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        mockChatResponseResult(jsonResponse);

        // When
        FilterResult result = filteringService.shouldProcess(bounty);

        // Then
        assertNotNull(result);
        assertTrue(result.shouldProcess());
        assertEquals(0.9, result.confidence(), 0.01);
        assertEquals(15, result.estimatedTimeMinutes());
        verify(chatClient, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should reject low-value or complex bounty")
    void shouldRejectLowValueOrComplexBounty() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-456")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("polar")
                .amount(new BigDecimal("30.00"))
                .title("Refactor entire authentication system")
                .description("Need to completely rewrite the authentication layer with new security standards")
                .status(BountyStatus.OPEN)
                .build();

        // Mock the result chain: chatResponse.getResult().getOutput() returns assistantMessage
        String jsonResponse = """
                {
                  "shouldProcess": false,
                  "confidence": 0.2,
                  "estimatedTimeMinutes": 480,
                  "reason": "Too complex, low value, requires extensive refactoring"
                }
                """;
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        mockChatResponseResult(jsonResponse);

        // When
        FilterResult result = filteringService.shouldProcess(bounty);

        // Then
        assertNotNull(result);
        assertFalse(result.shouldProcess());
        assertEquals(0.2, result.confidence(), 0.01);
        verify(chatClient, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should reject bounties without sufficient information")
    void shouldRejectBountiesWithoutInformation() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-789")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("100.00"))
                .status(BountyStatus.OPEN)
                .build();

        // Mock the result chain: chatResponse.getResult().getOutput() returns assistantMessage
        String jsonResponse = """
                {
                  "shouldProcess": false,
                  "confidence": 0.1,
                  "estimatedTimeMinutes": 0,
                  "reason": "Insufficient information to assess fixability"
                }
                """;
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        mockChatResponseResult(jsonResponse);

        // When
        FilterResult result = filteringService.shouldProcess(bounty);

        // Then
        assertNotNull(result);
        assertFalse(result.shouldProcess());
    }

    @Test
    @DisplayName("Should handle LLM errors gracefully")
    void shouldHandleLlmErrors() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-999")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        when(chatClient.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("LLM service unavailable"));

        // When
        FilterResult result = filteringService.shouldProcess(bounty);

        // Then
        assertNotNull(result);
        // Should default to false on error (fail-safe)
        assertFalse(result.shouldProcess());
        verify(chatClient, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should filter by minimum confidence threshold")
    void shouldFilterByConfidenceThreshold() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-111")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("100.00"))
                .title("Fix bug")
                .description("Some bug")
                .status(BountyStatus.OPEN)
                .build();

        // Mock the result chain: chatResponse.getResult().getOutput() returns assistantMessage
        // Note: This test checks that low confidence overrides shouldProcess=true
        // The LLM returns shouldProcess=true, but confidence 0.4 < 0.7 threshold, so it should be rejected
        String jsonResponse = """
                {
                  "shouldProcess": true,
                  "confidence": 0.4,
                  "estimatedTimeMinutes": 30,
                  "reason": "Possible fix but low confidence"
                }
                """;
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        mockChatResponseResult(jsonResponse);

        // When
        FilterResult result = filteringService.shouldProcess(bounty, 0.7); // High threshold

        // Then
        assertNotNull(result);
        // Even if LLM says shouldProcess=true, low confidence should override
        assertFalse(result.shouldProcess());
    }

    @Test
    @DisplayName("Should respect maximum time threshold")
    void shouldRespectMaximumTimeThreshold() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-222")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("200.00"))
                .title("Fix complex issue")
                .description("Complex issue requiring extensive work")
                .status(BountyStatus.OPEN)
                .build();

        // Mock the result chain: chatResponse.getResult().getOutput() returns assistantMessage
        // Note: This test checks that high time estimate overrides shouldProcess=true
        // The LLM returns shouldProcess=true, but 120 minutes > 30 minute threshold, so it should be rejected
        String jsonResponse = """
                {
                  "shouldProcess": true,
                  "confidence": 0.8,
                  "estimatedTimeMinutes": 120,
                  "reason": "Fixable but time-consuming"
                }
                """;
        when(chatClient.call(any(Prompt.class))).thenReturn(chatResponse);
        mockChatResponseResult(jsonResponse);

        // When - with 30 minute max threshold
        FilterResult result = filteringService.shouldProcess(bounty, 0.5, 30);

        // Then
        assertNotNull(result);
        assertFalse(result.shouldProcess()); // Exceeds time threshold
    }
}

