package com.bugbounty.bounty.service;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.service.impl.PolarApiClientImpl;
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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PolarApiClient Tests")
class PolarApiClientTest {

    private MockWebServer mockWebServer;
    private PolarApiClient apiClient;
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
        apiClient = new PolarApiClientImpl(webClient, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should fetch bounties from Polar API successfully")
    void shouldFetchBountiesSuccessfully() throws Exception {
        // Given
        String responseBody = """
                {
                  "items": [
                    {
                      "id": "polar-123",
                      "issue": {
                        "id": "issue-123",
                        "repository": {
                          "url": "https://github.com/owner/repo"
                        },
                        "title": "Fix authentication bug",
                        "body": "The authentication flow is broken"
                      },
                      "reward": {
                        "amount": 250.00,
                        "currency": "USD"
                      },
                      "state": "open"
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
        assertEquals("https://github.com/owner/repo", bounty.getRepositoryUrl());
        assertEquals("polar", bounty.getPlatform());
        assertEquals(0, new BigDecimal("250.00").compareTo(bounty.getAmount()));
        assertEquals("USD", bounty.getCurrency());
        assertEquals("Fix authentication bug", bounty.getTitle());
        assertEquals("The authentication flow is broken", bounty.getDescription());
        assertEquals(BountyStatus.OPEN, bounty.getStatus());

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("/api/v1/bounties"));
    }

    @Test
    @DisplayName("Should handle empty response from Polar API")
    void shouldHandleEmptyResponse() throws Exception {
        // Given
        String responseBody = """
                {
                  "items": []
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
    @DisplayName("Should handle pagination if present")
    void shouldHandlePagination() throws Exception {
        // Given
        String responseBody = """
                {
                  "items": [
                    {
                      "id": "polar-123",
                      "issue": {
                        "id": "issue-123",
                        "repository": {
                          "url": "https://github.com/owner/repo"
                        },
                        "title": "Fix bug 1"
                      },
                      "reward": {
                        "amount": 100.00,
                        "currency": "USD"
                      },
                      "state": "open"
                    }
                  ],
                  "pagination": {
                    "next": null
                  }
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
    }

    @Test
    @DisplayName("Should skip bounties without reward")
    void shouldSkipBountiesWithoutReward() throws Exception {
        // Given
        String responseBody = """
                {
                  "items": [
                    {
                      "id": "polar-123",
                      "issue": {
                        "id": "issue-123",
                        "repository": {
                          "url": "https://github.com/owner/repo"
                        },
                        "title": "Fix bug"
                      },
                      "state": "open"
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
        // Should skip bounties without reward amount
        assertTrue(result.isEmpty());
    }
}

