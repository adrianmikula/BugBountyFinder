package com.bugbounty.bounty.service.impl;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.service.GitPayApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of GitPayApiClient.
 * 
 * Note: GitPay API structure may need adjustment based on actual API documentation.
 * This implementation follows common REST API patterns and can be adapted.
 * 
 * Reference: https://gitpay.me/
 */
@Component
@Slf4j
public class GitPayApiClientImpl implements GitPayApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GitPayApiClientImpl(
            @Qualifier("gitpayWebClient") WebClient webClient,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "gitpayApi", fallbackMethod = "fetchBountiesFallback")
    @RateLimiter(name = "gitpayApi")
    public Flux<Bounty> fetchBounties() {
        log.debug("Fetching bounties from GitPay API");

        // GitPay API endpoint - may need adjustment based on actual API
        // Common patterns: /api/bounties, /api/tasks, /api/issues, /v1/bounties
        return webClient.get()
                .uri("/api/bounties?status=open")  // Adjust endpoint as needed
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        
                        // Try different response structures
                        JsonNode bountiesNode = root.has("bounties") ? root.get("bounties") :
                              root.has("items") ? root.get("items") :
                              root.has("data") ? root.get("data") :
                              root.isArray() ? root : null;
                        
                        if (bountiesNode == null || !bountiesNode.isArray()) {
                            log.warn("Invalid response format from GitPay API. Root structure: {}", root);
                            return Flux.empty();
                        }

                        List<Bounty> bounties = new ArrayList<>();
                        for (JsonNode bountyNode : bountiesNode) {
                            try {
                                Bounty bounty = parseBounty(bountyNode);
                                if (bounty != null) {
                                    bounties.add(bounty);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to parse bounty: {}", bountyNode, e);
                            }
                        }

                        return Flux.fromIterable(bounties);
                    } catch (Exception e) {
                        log.error("Failed to parse GitPay API response", e);
                        return Flux.error(e);
                    }
                })
                .doOnError(error -> log.error("Error fetching bounties from GitPay API", error));
    }

    /**
     * Parse a GitPay bounty JSON node to a Bounty domain object.
     * This may need adjustment based on actual GitPay API response structure.
     */
    private Bounty parseBounty(JsonNode node) {
        try {
            Bounty.BountyBuilder builder = Bounty.builder()
                    .platform("gitpay")
                    .status(BountyStatus.OPEN);

            // Extract issue ID (may be in different fields)
            if (node.has("issueId")) {
                builder.issueId(node.get("issueId").asText());
            } else if (node.has("issue_id")) {
                builder.issueId(node.get("issue_id").asText());
            } else if (node.has("taskId")) {
                builder.issueId(node.get("taskId").asText());
            } else if (node.has("id")) {
                builder.issueId(node.get("id").asText());
            } else {
                log.warn("Bounty missing issue ID");
                return null;
            }

            // Extract repository URL
            if (node.has("repositoryUrl")) {
                builder.repositoryUrl(node.get("repositoryUrl").asText());
            } else if (node.has("repository_url")) {
                builder.repositoryUrl(node.get("repository_url").asText());
            } else if (node.has("repoUrl")) {
                builder.repositoryUrl(node.get("repoUrl").asText());
            } else if (node.has("repository")) {
                JsonNode repoNode = node.get("repository");
                if (repoNode.has("url")) {
                    builder.repositoryUrl(repoNode.get("url").asText());
                } else if (repoNode.has("html_url")) {
                    builder.repositoryUrl(repoNode.get("html_url").asText());
                } else if (repoNode.isTextual()) {
                    builder.repositoryUrl(repoNode.asText());
                }
            }

            if (builder.build().getRepositoryUrl() == null) {
                log.warn("Bounty missing repository URL");
                return null;
            }

            // Extract amount
            if (node.has("amount")) {
                builder.amount(new BigDecimal(node.get("amount").asText()));
            } else if (node.has("reward")) {
                JsonNode rewardNode = node.get("reward");
                if (rewardNode.has("amount")) {
                    builder.amount(new BigDecimal(rewardNode.get("amount").asText()));
                } else if (rewardNode.isNumber()) {
                    builder.amount(new BigDecimal(rewardNode.asText()));
                }
            } else if (node.has("bounty")) {
                JsonNode bountyNode = node.get("bounty");
                if (bountyNode.has("amount")) {
                    builder.amount(new BigDecimal(bountyNode.get("amount").asText()));
                }
            }

            // Extract currency
            if (node.has("currency")) {
                builder.currency(node.get("currency").asText());
            } else if (node.has("reward") && node.get("reward").has("currency")) {
                builder.currency(node.get("reward").get("currency").asText());
            } else {
                builder.currency("USD"); // Default
            }

            // Extract title
            if (node.has("title")) {
                builder.title(node.get("title").asText());
            } else if (node.has("name")) {
                builder.title(node.get("name").asText());
            } else if (node.has("issue") && node.get("issue").has("title")) {
                builder.title(node.get("issue").get("title").asText());
            }

            // Extract description
            if (node.has("description")) {
                builder.description(node.get("description").asText());
            } else if (node.has("body")) {
                builder.description(node.get("body").asText());
            } else if (node.has("issue") && node.get("issue").has("body")) {
                builder.description(node.get("issue").get("body").asText());
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Error parsing GitPay bounty", e);
            return null;
        }
    }

    public Flux<Bounty> fetchBountiesFallback(Throwable throwable) {
        log.error("GitPay API circuit breaker opened or rate limited", throwable);
        return Flux.empty();
    }
}

