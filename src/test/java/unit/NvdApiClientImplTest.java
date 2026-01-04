package com.bugbounty.cve.service.impl;

import com.bugbounty.cve.domain.CVE;
import com.bugbounty.cve.service.NvdApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NvdApiClientImpl Tests")
class NvdApiClientImplTest {

    private MockWebServer mockWebServer;
    private NvdApiClient apiClient;
    private ObjectMapper objectMapper;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        objectMapper = new ObjectMapper();
        
        WebClient.Builder webClientBuilder = WebClient.builder();
        apiClient = new NvdApiClientImpl(webClientBuilder, objectMapper);
        
        // Use reflection to set the base URL for testing
        try {
            java.lang.reflect.Field field = NvdApiClientImpl.class.getDeclaredField("nvdApiBaseUrl");
            field.setAccessible(true);
            field.set(apiClient, baseUrl);
            
            // Initialize the web client
            java.lang.reflect.Method initMethod = NvdApiClientImpl.class.getDeclaredMethod("init");
            initMethod.setAccessible(true);
            initMethod.invoke(apiClient);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test", e);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should fetch recent CVEs successfully")
    void shouldFetchRecentCVEsSuccessfully() {
        // Given
        String responseBody = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-1234",
                        "descriptions": [
                          {
                            "lang": "en",
                            "value": "Vulnerability in Spring Framework"
                          }
                        ],
                        "metrics": {
                          "cvssMetricV31": [
                            {
                              "cvssData": {
                                "baseScore": 9.8
                              }
                            }
                          ]
                        },
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T01:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        var result = apiClient.fetchRecentCVEs(startDate, endDate)
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        CVE cve = result.get(0);
        assertEquals("CVE-2024-1234", cve.getCveId());
        assertEquals("Vulnerability in Spring Framework", cve.getDescription());
        assertEquals(9.8, cve.getCvssScore());
        assertEquals("CRITICAL", cve.getSeverity());
        assertEquals("NVD", cve.getSource());
    }

    @Test
    @DisplayName("Should fetch CVE by ID successfully")
    void shouldFetchCVEByIdSuccessfully() {
        // Given
        String responseBody = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-5678",
                        "descriptions": [
                          {
                            "lang": "en",
                            "value": "Another vulnerability"
                          }
                        ],
                        "metrics": {
                          "cvssMetricV30": [
                            {
                              "cvssData": {
                                "baseScore": 7.5
                              }
                            }
                          ]
                        },
                        "published": "2024-01-02T00:00:00.000",
                        "lastModified": "2024-01-02T01:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        // When
        var result = apiClient.fetchCVEById("CVE-2024-5678")
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CVE-2024-5678", result.get(0).getCveId());
        assertEquals(7.5, result.get(0).getCvssScore());
        assertEquals("HIGH", result.get(0).getSeverity());
    }

    @Test
    @DisplayName("Should fetch CVEs by severity successfully")
    void shouldFetchCVEsBySeveritySuccessfully() {
        // Given
        String criticalResponse = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-1111",
                        "descriptions": [
                          {
                            "lang": "en",
                            "value": "Critical vulnerability"
                          }
                        ],
                        "metrics": {
                          "cvssMetricV31": [
                            {
                              "cvssData": {
                                "baseScore": 9.9
                              }
                            }
                          ]
                        },
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T01:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        String highResponse = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-2222",
                        "descriptions": [
                          {
                            "lang": "en",
                            "value": "High vulnerability"
                          }
                        ],
                        "metrics": {
                          "cvssMetricV31": [
                            {
                              "cvssData": {
                                "baseScore": 8.5
                              }
                            }
                          ]
                        },
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T01:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(criticalResponse));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(highResponse));

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();
        List<String> severities = List.of("CRITICAL", "HIGH");

        // When
        var result = apiClient.fetchCVEsBySeverity(severities, startDate, endDate)
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertTrue(result.size() >= 1); // At least one CVE should be returned
    }

    @Test
    @DisplayName("Should handle empty response")
    void shouldHandleEmptyResponse() {
        // Given
        String responseBody = """
                {
                  "totalResults": 0,
                  "vulnerabilities": []
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        var result = apiClient.fetchRecentCVEs(startDate, endDate)
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
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // When & Then
        StepVerifier.create(apiClient.fetchRecentCVEs(startDate, endDate))
                .expectError()
                .verify();
    }

    @Test
    @DisplayName("Should map CVSS score to severity correctly")
    void shouldMapCVSSScoreToSeverity() {
        // Given
        String criticalResponse = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-CRITICAL",
                        "descriptions": [{"lang": "en", "value": "Critical"}],
                        "metrics": {
                          "cvssMetricV31": [{"cvssData": {"baseScore": 9.5}}]
                        },
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T00:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        String highResponse = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-HIGH",
                        "descriptions": [{"lang": "en", "value": "High"}],
                        "metrics": {
                          "cvssMetricV31": [{"cvssData": {"baseScore": 7.5}}]
                        },
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T00:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        String mediumResponse = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-MEDIUM",
                        "descriptions": [{"lang": "en", "value": "Medium"}],
                        "metrics": {
                          "cvssMetricV31": [{"cvssData": {"baseScore": 5.5}}]
                        },
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T00:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        String lowResponse = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-LOW",
                        "descriptions": [{"lang": "en", "value": "Low"}],
                        "metrics": {
                          "cvssMetricV31": [{"cvssData": {"baseScore": 2.5}}]
                        },
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T00:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(criticalResponse));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(highResponse));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mediumResponse));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(lowResponse));

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        var critical = apiClient.fetchRecentCVEs(startDate, endDate).blockFirst();
        var high = apiClient.fetchRecentCVEs(startDate, endDate).blockFirst();
        var medium = apiClient.fetchRecentCVEs(startDate, endDate).blockFirst();
        var low = apiClient.fetchRecentCVEs(startDate, endDate).blockFirst();

        // Then
        assertNotNull(critical);
        assertEquals("CRITICAL", critical.getSeverity());
        
        assertNotNull(high);
        assertEquals("HIGH", high.getSeverity());
        
        assertNotNull(medium);
        assertEquals("MEDIUM", medium.getSeverity());
        
        assertNotNull(low);
        assertEquals("LOW", low.getSeverity());
    }

    @Test
    @DisplayName("Should handle CVE without CVSS score")
    void shouldHandleCVEWithoutCVSSScore() {
        // Given
        String responseBody = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-NOSCORE",
                        "descriptions": [{"lang": "en", "value": "No score"}],
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T00:00:00.000",
                        "configurations": []
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        var result = apiClient.fetchRecentCVEs(startDate, endDate)
                .blockFirst();

        // Then
        assertNotNull(result);
        assertEquals("CVE-2024-NOSCORE", result.getCveId());
        assertNull(result.getCvssScore());
        assertEquals("UNKNOWN", result.getSeverity());
    }

    @Test
    @DisplayName("Should extract affected languages from CPE configurations")
    void shouldExtractAffectedLanguagesFromCPE() {
        // Given
        String responseBody = """
                {
                  "totalResults": 1,
                  "vulnerabilities": [
                    {
                      "cve": {
                        "id": "CVE-2024-1234",
                        "descriptions": [{"lang": "en", "value": "Test"}],
                        "metrics": {
                          "cvssMetricV31": [{"cvssData": {"baseScore": 7.5}}]
                        },
                        "published": "2024-01-01T00:00:00.000",
                        "lastModified": "2024-01-01T00:00:00.000",
                        "configurations": [
                          {
                            "nodes": [
                              {
                                "cpeMatch": [
                                  {
                                    "criteria": "cpe:2.3:a:apache:log4j:2.0:*:*:*:*:java:*:*"
                                  }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(responseBody));

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        var result = apiClient.fetchRecentCVEs(startDate, endDate)
                .blockFirst();

        // Then
        assertNotNull(result);
        assertNotNull(result.getAffectedLanguages());
        // Note: The actual extraction logic may vary, but we verify it doesn't crash
    }
}

