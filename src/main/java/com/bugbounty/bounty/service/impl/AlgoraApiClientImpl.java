package com.bugbounty.bounty.service.impl;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.service.AlgoraApiClient;
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
public class AlgoraApiClientImpl implements AlgoraApiClient {

    @org.springframework.beans.factory.annotation.Qualifier("algoraWebClient")
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Override
    @CircuitBreaker(name = "algoraApi", fallbackMethod = "fetchBountiesFallback")
    @RateLimiter(name = "algoraApi")
    public Flux<Bounty> fetchBounties() {
        log.debug("Fetching bounties from Algora API");

        return webClient.get()
                .uri("/v1/bounties")
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        JsonNode bountiesNode = root.get("bounties");
                        
                        if (bountiesNode == null || !bountiesNode.isArray()) {
                            log.warn("Invalid response format from Algora API");
                            return Flux.empty();
                        }

                        List<Bounty> bounties = new ArrayList<>();
                        for (JsonNode bountyNode : bountiesNode) {
                            try {
                                Bounty bounty = parseBounty(bountyNode);
                                bounties.add(bounty);
                            } catch (Exception e) {
                                log.warn("Failed to parse bounty: {}", bountyNode, e);
                            }
                        }

                        return Flux.fromIterable(bounties);
                    } catch (Exception e) {
                        log.error("Failed to parse Algora API response", e);
                        return Flux.error(e);
                    }
                })
                .doOnError(error -> log.error("Error fetching bounties from Algora API", error));
    }

    private Bounty parseBounty(JsonNode node) {
        Bounty.BountyBuilder builder = Bounty.builder()
                .issueId(node.get("issueId").asText())
                .repositoryUrl(node.get("repositoryUrl").asText())
                .platform("algora")
                .status(BountyStatus.OPEN);

        if (node.has("id")) {
            // Could map to internal ID if needed
        }

        if (node.has("amount")) {
            builder.amount(new BigDecimal(node.get("amount").asText()));
        }

        if (node.has("currency")) {
            builder.currency(node.get("currency").asText());
        }

        if (node.has("title")) {
            builder.title(node.get("title").asText());
        }

        if (node.has("description")) {
            builder.description(node.get("description").asText());
        }

        return builder.build();
    }

    public Flux<Bounty> fetchBountiesFallback(Throwable throwable) {
        log.error("Algora API circuit breaker opened or rate limited", throwable);
        return Flux.empty();
    }
}

