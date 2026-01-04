package com.bugbounty.component;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.triage.BountyFilteringService;
import com.bugbounty.bounty.triage.FilterResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Component test for BountyFilteringService.
 * Tests LLM integration with mocked ChatClient.
 */
@DisplayName("Bounty Filtering Component Tests")
class BountyFilteringComponentTest extends AbstractComponentTest {

    @Autowired
    private BountyFilteringService filteringService;

    // Note: ChatClient is provided as a default mock by ComponentTestConfiguration
    // We override it here with @MockBean to set up specific behavior for this test
    @MockBean
    private ChatClient chatClient;

    @BeforeEach
    void setUp() {
        reset(chatClient);
    }

    /**
     * Helper method to mock the ChatResponse result chain.
     * Uses reflection to work around the inaccessible Generation type.
     */
    @SuppressWarnings({"unchecked"})
    private void mockChatResponseResult(ChatResponse mockResponse, AssistantMessage mockAssistantMessage) {
        try {
            java.lang.reflect.Method getResultMethod = ChatResponse.class.getMethod("getResult");
            Class<?> resultType = getResultMethod.getReturnType();
            
            // Create a mock for the result with Answer to intercept method calls
            Object resultMock = mock(resultType, invocation -> {
                java.lang.reflect.Method method = invocation.getMethod();
                if ("getOutput".equals(method.getName())) {
                    return mockAssistantMessage;
                }
                // For other methods, return default mock behavior
                return null;
            });
            
            // Use doReturn to set up getResult() to return our mock
            lenient().doReturn(resultMock).when(mockResponse).getResult();
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

        ChatResponse mockResponse = mock(ChatResponse.class);
        AssistantMessage mockAssistantMessage = mock(AssistantMessage.class);

        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);
        mockChatResponseResult(mockResponse, mockAssistantMessage);
        when(mockAssistantMessage.getContent()).thenReturn("""
                {
                  "shouldProcess": true,
                  "confidence": 0.9,
                  "estimatedTimeMinutes": 15,
                  "reason": "Simple typo fix, low complexity, high confidence"
                }
                """);

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
                .description("Need to completely rewrite the authentication layer")
                .status(BountyStatus.OPEN)
                .build();

        ChatResponse mockResponse = mock(ChatResponse.class);
        AssistantMessage mockAssistantMessage = mock(AssistantMessage.class);

        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);
        mockChatResponseResult(mockResponse, mockAssistantMessage);
        when(mockAssistantMessage.getContent()).thenReturn("""
                {
                  "shouldProcess": false,
                  "confidence": 0.2,
                  "estimatedTimeMinutes": 480,
                  "reason": "Too complex, low value, requires extensive refactoring"
                }
                """);

        // When
        FilterResult result = filteringService.shouldProcess(bounty);

        // Then
        assertNotNull(result);
        assertFalse(result.shouldProcess());
        assertEquals(0.2, result.confidence(), 0.01);
        verify(chatClient, times(1)).call(any(Prompt.class));
    }

    @Test
    @DisplayName("Should apply confidence threshold")
    void shouldApplyConfidenceThreshold() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-789")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("100.00"))
                .title("Fix bug")
                .description("Some bug")
                .status(BountyStatus.OPEN)
                .build();

        ChatResponse mockResponse = mock(ChatResponse.class);
        AssistantMessage mockAssistantMessage = mock(AssistantMessage.class);

        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);
        mockChatResponseResult(mockResponse, mockAssistantMessage);
        when(mockAssistantMessage.getContent()).thenReturn("""
                {
                  "shouldProcess": true,
                  "confidence": 0.4,
                  "estimatedTimeMinutes": 30,
                  "reason": "Possible fix but low confidence"
                }
                """);

        // When - with high threshold
        FilterResult result = filteringService.shouldProcess(bounty, 0.7);

        // Then
        assertNotNull(result);
        assertFalse(result.shouldProcess()); // Rejected due to low confidence
    }

    @Test
    @DisplayName("Should apply time threshold")
    void shouldApplyTimeThreshold() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-999")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("200.00"))
                .title("Fix complex issue")
                .description("Complex issue requiring extensive work")
                .status(BountyStatus.OPEN)
                .build();

        ChatResponse mockResponse = mock(ChatResponse.class);
        AssistantMessage mockAssistantMessage = mock(AssistantMessage.class);

        when(chatClient.call(any(Prompt.class))).thenReturn(mockResponse);
        mockChatResponseResult(mockResponse, mockAssistantMessage);
        when(mockAssistantMessage.getContent()).thenReturn("""
                {
                  "shouldProcess": true,
                  "confidence": 0.8,
                  "estimatedTimeMinutes": 120,
                  "reason": "Fixable but time-consuming"
                }
                """);

        // When - with 30 minute max threshold
        FilterResult result = filteringService.shouldProcess(bounty, 0.5, 30);

        // Then
        assertNotNull(result);
        assertFalse(result.shouldProcess()); // Rejected due to time threshold
    }

    @Test
    @DisplayName("Should handle LLM errors gracefully")
    void shouldHandleLlmErrors() {
        // Given
        Bounty bounty = Bounty.builder()
                .issueId("issue-error")
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
        assertFalse(result.shouldProcess()); // Fail-safe: reject on error
        assertThat(result.reason()).contains("Error");
    }
}

