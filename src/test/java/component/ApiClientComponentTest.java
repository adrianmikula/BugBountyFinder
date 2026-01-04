package com.bugbounty.component;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.service.impl.AlgoraApiClientImpl;
import com.bugbounty.bounty.service.impl.PolarApiClientImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Component test for API clients.
 * Tests HTTP client integration with MockWebServer.
 */
@DisplayName("API Client Component Tests")
class ApiClientComponentTest extends AbstractComponentTest {

    private MockWebServer mockWebServer;
    private AlgoraApiClientImpl algoraApiClient;
    private PolarApiClientImpl polarApiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
        
        objectMapper = new ObjectMapper();
        algoraApiClient = new AlgoraApiClientImpl(webClient, objectMapper);
        polarApiClient = new PolarApiClientImpl(webClient, objectMapper);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @Test
    @DisplayName("Should fetch bounties from Algora API")
    void shouldFetchBountiesFromAlgora() throws Exception {
        // Given
        String responseBody = """
                {
                  "bounties": [
                    {
                      "id": "algora-123",
                      "issueId": "issue-123",
                      "repositoryUrl": "https://github.com/owner/repo",
                      "amount": 150.00,
                      "currency": "USD",
                      "title": "Fix bug",
                      "description": "Bug description",
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
        List<Bounty> bounties = algoraApiClient.fetchBounties()
                .collectList()
                .block();

        // Then
        assertNotNull(bounties);
        assertEquals(1, bounties.size());
        
        Bounty bounty = bounties.get(0);
        assertEquals("issue-123", bounty.getIssueId());
        assertEquals("algora", bounty.getPlatform());
        assertEquals("https://github.com/owner/repo", bounty.getRepositoryUrl());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/v1/bounties", request.getPath());
    }

    @Test
    @DisplayName("Should fetch bounties from Polar API")
    void shouldFetchBountiesFromPolar() throws Exception {
        // Given
        String responseBody = """
                {
                  "items": [
                    {
                      "id": "polar-123",
                      "issue": {
                        "id": "issue-456",
                        "repository": {
                          "url": "https://github.com/owner/repo"
                        },
                        "title": "Fix authentication",
                        "body": "Auth bug description"
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
        List<Bounty> bounties = polarApiClient.fetchBounties()
                .collectList()
                .block();

        // Then
        assertNotNull(bounties);
        assertEquals(1, bounties.size());
        
        Bounty bounty = bounties.get(0);
        assertEquals("issue-456", bounty.getIssueId());
        assertEquals("polar", bounty.getPlatform());
        assertEquals("https://github.com/owner/repo", bounty.getRepositoryUrl());

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("/api/v1/bounties");
    }

    @Test
    @DisplayName("Should handle API errors gracefully")
    void shouldHandleApiErrors() {
        // Given
        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        // When & Then
        StepVerifier.create(algoraApiClient.fetchBounties())
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Should handle rate limiting")
    void shouldHandleRateLimiting() {
        // Given
        mockWebServer.enqueue(new okhttp3.mockwebserver.MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "60")
                .setBody("Rate limit exceeded"));

        // When & Then
        StepVerifier.create(algoraApiClient.fetchBounties())
                .expectError()
                .verify();
    }
}

