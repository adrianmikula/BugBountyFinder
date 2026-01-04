package com.bugbounty.webhook.controller;

import com.bugbounty.webhook.dto.GitHubIssueEvent;
import com.bugbounty.webhook.dto.GitHubPushEvent;
import com.bugbounty.webhook.service.GitHubWebhookService;
import com.bugbounty.webhook.service.WebhookSignatureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GitHubWebhookController.class)
@DisplayName("GitHubWebhookController Tests")
class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookSignatureService signatureService;

    @MockBean
    private GitHubWebhookService webhookService;

    @Autowired
    private ObjectMapper objectMapper;

    private String validPayload;
    private String validIssuePayload;

    @BeforeEach
    void setUp() {
        GitHubPushEvent pushEvent = new GitHubPushEvent();
        pushEvent.setRef("refs/heads/main");
        
        GitHubPushEvent.Repository repo = new GitHubPushEvent.Repository();
        repo.setFullName("owner/test-repo");
        repo.setCloneUrl("https://github.com/owner/test-repo.git");
        pushEvent.setRepository(repo);
        
        validPayload = """
                {
                  "ref": "refs/heads/main",
                  "repository": {
                    "id": 123456,
                    "name": "test-repo",
                    "full_name": "owner/test-repo",
                    "clone_url": "https://github.com/owner/test-repo.git"
                  },
                  "commits": []
                }
                """;
        
        validIssuePayload = """
                {
                  "action": "opened",
                  "issue": {
                    "id": 123456,
                    "number": 42,
                    "title": "Fix security vulnerability - $500 bounty",
                    "body": "This issue has a $500 bounty attached.",
                    "state": "open",
                    "created_at": "2024-01-01T12:00:00Z",
                    "updated_at": "2024-01-01T12:00:00Z"
                  },
                  "repository": {
                    "id": 789012,
                    "name": "test-repo",
                    "full_name": "owner/test-repo",
                    "clone_url": "https://github.com/owner/test-repo.git",
                    "html_url": "https://github.com/owner/test-repo"
                  }
                }
                """;
    }

    @Test
    @DisplayName("Should handle valid push event")
    void shouldHandleValidPushEvent() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        when(webhookService.processPushEvent(any(GitHubPushEvent.class))).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));
    }

    @Test
    @DisplayName("Should reject invalid signature")
    void shouldRejectInvalidSignature() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=invalid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid signature"));
    }

    @Test
    @DisplayName("Should reject non-push events")
    void shouldRejectNonPushEvents() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle ping events")
    void shouldHandlePingEvents() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/ping")
                        .header("X-GitHub-Event", "ping")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zen\": \"test\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Pong"));
    }

    @Test
    @DisplayName("Should return health check")
    void shouldReturnHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/webhooks/github/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("GitHub webhook endpoint is active"));
    }

    @Test
    @DisplayName("Should handle processing failure gracefully")
    void shouldHandleProcessingFailure() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        when(webhookService.processPushEvent(any(GitHubPushEvent.class))).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to process webhook"));
    }

    @Test
    @DisplayName("Should handle missing X-GitHub-Event header")
    void shouldHandleMissingEventHeader() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle malformed JSON payload")
    void shouldHandleMalformedJsonPayload() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle empty payload")
    void shouldHandleEmptyPayload() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle ping event with invalid signature")
    void shouldHandlePingEventWithInvalidSignature() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/ping")
                        .header("X-GitHub-Event", "ping")
                        .header("X-Hub-Signature-256", "sha256=invalid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zen\": \"test\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid signature"));
    }

    @Test
    @DisplayName("Should handle exception during payload parsing")
    void shouldHandleExceptionDuringPayloadParsing() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        // ObjectMapper will throw exception for invalid JSON
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json at all"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle exception during event processing")
    void shouldHandleExceptionDuringEventProcessing() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        when(webhookService.processPushEvent(any(GitHubPushEvent.class)))
                .thenThrow(new RuntimeException("Processing error"));
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/push")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload))
                .andExpect(status().isInternalServerError());
    }

    // Issue Event Tests

    @Test
    @DisplayName("Should handle valid issue event")
    void shouldHandleValidIssueEvent() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        when(webhookService.processIssueEvent(any(GitHubIssueEvent.class))).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/issues")
                        .header("X-GitHub-Event", "issues")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssuePayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));
    }

    @Test
    @DisplayName("Should reject invalid signature for issue event")
    void shouldRejectInvalidSignatureForIssueEvent() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/issues")
                        .header("X-GitHub-Event", "issues")
                        .header("X-Hub-Signature-256", "sha256=invalid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssuePayload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid signature"));
    }

    @Test
    @DisplayName("Should reject non-issues events")
    void shouldRejectNonIssuesEvents() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/issues")
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssuePayload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Expected 'issues' event")));
    }

    @Test
    @DisplayName("Should handle issue event processing failure")
    void shouldHandleIssueEventProcessingFailure() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        when(webhookService.processIssueEvent(any(GitHubIssueEvent.class))).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/issues")
                        .header("X-GitHub-Event", "issues")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssuePayload))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to process webhook"));
    }

    @Test
    @DisplayName("Should handle malformed JSON payload for issue event")
    void shouldHandleMalformedJsonPayloadForIssueEvent() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/issues")
                        .header("X-GitHub-Event", "issues")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should handle exception during issue event processing")
    void shouldHandleExceptionDuringIssueEventProcessing() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        when(webhookService.processIssueEvent(any(GitHubIssueEvent.class)))
                .thenThrow(new RuntimeException("Processing error"));
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github/issues")
                        .header("X-GitHub-Event", "issues")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssuePayload))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should route issue events through unified webhook endpoint")
    void shouldRouteIssueEventsThroughUnifiedWebhookEndpoint() throws Exception {
        // Given
        when(signatureService.verifySignature(anyString(), anyString())).thenReturn(true);
        when(webhookService.processIssueEvent(any(GitHubIssueEvent.class))).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/api/webhooks/github")
                        .header("X-GitHub-Event", "issues")
                        .header("X-Hub-Signature-256", "sha256=valid_signature")
                        .header("X-GitHub-Delivery", "test-delivery-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validIssuePayload))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed successfully"));
    }
}

