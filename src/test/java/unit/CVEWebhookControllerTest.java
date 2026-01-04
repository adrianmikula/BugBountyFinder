package com.bugbounty.cve.webhook.controller;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.service.CVEMonitoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CVEWebhookController.class)
@DisplayName("CVEWebhookController Tests")
class CVEWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CVEMonitoringService cveMonitoringService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        doNothing().when(cveMonitoringService).handleCVEWebhook(any(CVE.class));
    }

    @Test
    @DisplayName("Should handle valid CVE webhook payload")
    void shouldHandleValidCVEWebhookPayload() throws Exception {
        // Given
        String payload = """
                {
                  "cveId": "CVE-2024-1234",
                  "description": "Vulnerability in Spring Framework",
                  "severity": "CRITICAL",
                  "cvssScore": 9.8,
                  "publishedDate": "2024-01-01T00:00:00",
                  "affectedLanguages": ["Java"],
                  "affectedProducts": ["Spring Framework"]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("CVE webhook processed successfully"));

        verify(cveMonitoringService).handleCVEWebhook(any(CVE.class));
    }

    @Test
    @DisplayName("Should handle webhook with alternative field names")
    void shouldHandleWebhookWithAlternativeFieldNames() throws Exception {
        // Given
        String payload = """
                {
                  "cve_id": "CVE-2024-5678",
                  "description": "Another vulnerability",
                  "cvss_severity": "HIGH",
                  "cvss_score": 7.5,
                  "published_date": "2024-01-02T00:00:00",
                  "affected_languages": ["Python"],
                  "affected_products": ["Django"]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(cveMonitoringService).handleCVEWebhook(any(CVE.class));
    }

    @Test
    @DisplayName("Should return 400 when payload is missing cveId")
    void shouldReturn400WhenPayloadMissingCveId() throws Exception {
        // Given
        String payload = """
                {
                  "description": "Vulnerability description",
                  "severity": "HIGH"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(cveMonitoringService, never()).handleCVEWebhook(any(CVE.class));
    }

    @Test
    @DisplayName("Should return 400 when payload has empty cveId")
    void shouldReturn400WhenPayloadHasEmptyCveId() throws Exception {
        // Given
        String payload = """
                {
                  "cveId": "",
                  "severity": "HIGH"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(cveMonitoringService, never()).handleCVEWebhook(any(CVE.class));
    }

    @Test
    @DisplayName("Should handle webhook with minimal required fields")
    void shouldHandleWebhookWithMinimalRequiredFields() throws Exception {
        // Given
        String payload = """
                {
                  "cveId": "CVE-2024-9999",
                  "severity": "MEDIUM"
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(cveMonitoringService).handleCVEWebhook(any(CVE.class));
    }

    @Test
    @DisplayName("Should return 500 when service throws exception")
    void shouldReturn500WhenServiceThrowsException() throws Exception {
        // Given
        String payload = """
                {
                  "cveId": "CVE-2024-1234",
                  "severity": "HIGH"
                }
                """;

        doThrow(new RuntimeException("Service error"))
                .when(cveMonitoringService).handleCVEWebhook(any(CVE.class));

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should return health check response")
    void shouldReturnHealthCheckResponse() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/webhooks/cve/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("CVE webhook endpoint is active"));
    }

    @Test
    @DisplayName("Should handle webhook with array of languages")
    void shouldHandleWebhookWithArrayOfLanguages() throws Exception {
        // Given
        String payload = """
                {
                  "cveId": "CVE-2024-1234",
                  "severity": "CRITICAL",
                  "affectedLanguages": ["Java", "Python", "JavaScript"],
                  "affectedProducts": ["Spring", "Django", "React"]
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(cveMonitoringService).handleCVEWebhook(any(CVE.class));
    }

    @Test
    @DisplayName("Should handle webhook with numeric severity")
    void shouldHandleWebhookWithNumericSeverity() throws Exception {
        // Given
        String payload = """
                {
                  "cveId": "CVE-2024-1234",
                  "severity": 9.8
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        verify(cveMonitoringService).handleCVEWebhook(any(CVE.class));
    }

    @Test
    @DisplayName("Should handle invalid JSON payload")
    void shouldHandleInvalidJsonPayload() throws Exception {
        // Given
        String payload = "invalid json";

        // When & Then
        mockMvc.perform(post("/api/webhooks/cve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isInternalServerError());

        verify(cveMonitoringService, never()).handleCVEWebhook(any(CVE.class));
    }
}

