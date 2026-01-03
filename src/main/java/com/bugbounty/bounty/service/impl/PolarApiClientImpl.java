package com.bugbounty.bounty.service.impl;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.service.PolarApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PolarApiClientImpl implements PolarApiClient {

    @org.springframework.beans.factory.annotation.Qualifier("polarWebClient")
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "polarApi", fallbackMethod = "fetchBountiesFallback")
    @RateLimiter(name = "polarApi")
    public Flux<Bounty> fetchBounties() {
        log.debug("Fetching bounties from Polar API");

        return webClient.get()
                .uri("/api/v1/bounties?state=open")
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        JsonNode itemsNode = root.get("items");
                        
                        if (itemsNode == null || !itemsNode.isArray()) {
                            log.warn("Invalid response format from Polar API");
                            return Flux.empty();
                        }

                        List<Bounty> bounties = new ArrayList<>();
                        for (JsonNode itemNode : itemsNode) {
                            try {
                                Bounty bounty = parseBounty(itemNode);
                                if (bounty != null) {
                                    bounties.add(bounty);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to parse bounty: {}", itemNode, e);
                            }
                        }

                        return Flux.fromIterable(bounties);
                    } catch (Exception e) {
                        log.error("Failed to parse Polar API response", e);
                        return Flux.error(e);
                    }
                })
                .doOnError(error -> log.error("Error fetching bounties from Polar API", error));
    }

    private Bounty parseBounty(JsonNode node) {
        JsonNode issueNode = node.get("issue");
        JsonNode rewardNode = node.get("reward");

        if (issueNode == null) {
            log.warn("Bounty missing issue node");
            return null;
        }

        // Skip bounties without reward amount
        if (rewardNode == null || !rewardNode.has("amount")) {
            log.debug("Skipping bounty without reward amount");
            return null;
        }

        JsonNode repositoryNode = issueNode.get("repository");
        if (repositoryNode == null || !repositoryNode.has("url")) {
            log.warn("Bounty missing repository URL");
            return null;
        }

        Bounty.BountyBuilder builder = Bounty.builder()
                .issueId(issueNode.get("id").asText())
                .repositoryUrl(repositoryNode.get("url").asText())
                .platform("polar")
                .status(BountyStatus.OPEN);

        // Parse reward
        builder.amount(new BigDecimal(rewardNode.get("amount").asText()));
        if (rewardNode.has("currency")) {
            builder.currency(rewardNode.get("currency").asText());
        } else {
            builder.currency("USD"); // Default
        }

        // Parse issue details
        if (issueNode.has("title")) {
            builder.title(issueNode.get("title").asText());
        }

        if (issueNode.has("body")) {
            builder.description(issueNode.get("body").asText());
        }

        return builder.build();
    }

    public Flux<Bounty> fetchBountiesFallback(Throwable throwable) {
        log.error("Polar API circuit breaker opened or rate limited", throwable);
        return Flux.empty();
    }
}

