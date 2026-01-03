package com.bugbounty.bounty.service;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.service.impl.AlgoraApiClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AlgoraApiClient Tests")
class AlgoraApiClientTest {

    private MockWebServer mockWebServer;
    private AlgoraApiClient apiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        
        objectMapper = new ObjectMapper();
        apiClient = new AlgoraApiClientImpl(webClient, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should fetch bounties from Algora API successfully")
    void shouldFetchBountiesSuccessfully() throws Exception {
        // Given
        String responseBody = """
                {
                  "bounties": [
                    {
                      "id": "bounty-123",
                      "issueId": "issue-123",
                      "repositoryUrl": "https://github.com/owner/repo",
                      "amount": 150.00,
                      "currency": "USD",
                      "title": "Fix React hydration error",
                      "description": "The component has a hydration mismatch",
                      "status": "open"
                    },
                    {
                      "id": "bounty-456",
                      "issueId": "issue-456",
                      "repositoryUrl": "https://github.com/owner/repo2",
                      "amount": 200.00,
                      "currency": "USD",
                      "title": "Fix memory leak",
                      "description": "Memory leak in service layer",
                      "status": "open"
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        // When
        var result = apiClient.fetchBounties()
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        Bounty first = result.get(0);
        assertEquals("issue-123", first.getIssueId());
        assertEquals("https://github.com/owner/repo", first.getRepositoryUrl());
        assertEquals("algora", first.getPlatform());
        assertEquals(0, new BigDecimal("150.00").compareTo(first.getAmount()));
        assertEquals("Fix React hydration error", first.getTitle());
        assertEquals(BountyStatus.OPEN, first.getStatus());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/v1/bounties", request.getPath());
    }

    @Test
    @DisplayName("Should handle empty response from Algora API")
    void shouldHandleEmptyResponse() throws Exception {
        // Given
        String responseBody = """
                {
                  "bounties": []
                }
                """;

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        // When
        var result = apiClient.fetchBounties()
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle API errors gracefully")
    void shouldHandleApiErrors() {
        // Given
        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        // When & Then
        StepVerifier.create(apiClient.fetchBounties())
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Should handle rate limiting (429)")
    void shouldHandleRateLimiting() {
        // Given
        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "60")
                .setBody("Rate limit exceeded"));

        // When & Then
        StepVerifier.create(apiClient.fetchBounties())
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Should parse bounty with missing optional fields")
    void shouldParseBountyWithMissingFields() throws Exception {
        // Given
        String responseBody = """
                {
                  "bounties": [
                    {
                      "id": "bounty-123",
                      "issueId": "issue-123",
                      "repositoryUrl": "https://github.com/owner/repo",
                      "status": "open"
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        // When
        var result = apiClient.fetchBounties()
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        Bounty bounty = result.get(0);
        assertEquals("issue-123", bounty.getIssueId());
        assertNull(bounty.getAmount());
        assertNull(bounty.getTitle());
    }

    @Test
    @DisplayName("Should map different currency codes")
    void shouldMapDifferentCurrencies() throws Exception {
        // Given
        String responseBody = """
                {
                  "bounties": [
                    {
                      "id": "bounty-123",
                      "issueId": "issue-123",
                      "repositoryUrl": "https://github.com/owner/repo",
                      "amount": 100.00,
                      "currency": "EUR",
                      "status": "open"
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        // When
        var result = apiClient.fetchBounties()
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("EUR", result.get(0).getCurrency());
    }
}

