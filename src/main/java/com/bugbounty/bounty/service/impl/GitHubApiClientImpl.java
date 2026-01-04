package com.bugbounty.bounty.service.impl;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.service.GitHubApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of GitHubApiClient that scans GitHub issues for dollar amounts.
 * Looks for patterns like "$50", "$100", "$500" in issue titles and bodies.
 */
@Component
@Slf4j
public class GitHubApiClientImpl implements GitHubApiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // Pattern to match dollar amounts: $50, $100, $500, $1,000, etc.
    private static final Pattern BOUNTY_PATTERN = Pattern.compile(
        "\\$([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", 
        Pattern.CASE_INSENSITIVE
    );
    
    @Value("${app.bounty.platforms.github.rate-limit-per-hour:5000}")
    private int rateLimitPerHour;

    public GitHubApiClientImpl(
            @Qualifier("githubWebClient") WebClient webClient,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "githubApi", fallbackMethod = "fetchBountiesFromRepositoryFallback")
    @RateLimiter(name = "githubApi")
    public Flux<Bounty> fetchBountiesFromRepository(String owner, String repo) {
        log.debug("Fetching issues with bounties from repository {}/{}", owner, repo);
        
        return fetchIssues(owner, repo, 1)
                .flatMap(issue -> {
                    Bounty bounty = parseIssueToBounty(owner, repo, issue);
                    return bounty != null ? Flux.just(bounty) : Flux.empty();
                })
                .retryWhen(Retry.backoff(3, java.time.Duration.ofSeconds(2))
                        .filter(throwable -> {
                            // Retry on rate limit or server errors
                            if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                org.springframework.web.reactive.function.client.WebClientResponseException ex = 
                                    (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                return ex.getStatusCode().is5xxServerError() || 
                                       ex.getStatusCode().value() == 429; // Rate limit
                            }
                            return false;
                        }))
                .doOnError(error -> log.error("Error fetching issues from {}/{}", owner, repo, error));
    }

    @Override
    public Flux<Bounty> fetchBountiesFromRepositories(List<String> repositories) {
        log.debug("Fetching bounties from {} repositories", repositories.size());
        
        return Flux.fromIterable(repositories)
                .flatMap(repoIdentifier -> {
                    String[] parts = repoIdentifier.split("/");
                    if (parts.length != 2) {
                        log.warn("Invalid repository format: {}", repoIdentifier);
                        return Flux.<Bounty>empty();
                    }
                    return fetchBountiesFromRepository(parts[0], parts[1]);
                }, 5) // Process up to 5 repositories concurrently
                .doOnError(error -> log.error("Error fetching bounties from repositories", error));
    }

    /**
     * Fetch issues from GitHub API with pagination support.
     */
    private Flux<JsonNode> fetchIssues(String owner, String repo, int page) {
        String uri = String.format("/repos/%s/%s/issues?state=open&page=%d&per_page=100&sort=updated&direction=desc", 
                owner, repo, page);
        
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapMany(response -> {
                    try {
                        JsonNode issues = objectMapper.readTree(response);
                        if (!issues.isArray() || issues.size() == 0) {
                            return Flux.empty();
                        }
                        
                        List<JsonNode> issueList = new ArrayList<>();
                        issues.forEach(issueList::add);
                        Flux<JsonNode> currentPage = Flux.fromIterable(issueList);
                        
                        // If we got 100 issues, there might be more pages
                        if (issues.size() == 100) {
                            return currentPage.concatWith(fetchIssues(owner, repo, page + 1));
                        }
                        
                        return currentPage;
                    } catch (Exception e) {
                        log.error("Failed to parse GitHub API response", e);
                        return Flux.error(e);
                    }
                });
    }

    /**
     * Parse a GitHub issue JSON node to a Bounty if it contains a dollar amount.
     */
    private Bounty parseIssueToBounty(String owner, String repo, JsonNode issue) {
        // Skip pull requests (GitHub API returns both issues and PRs)
        if (issue.has("pull_request")) {
            return null;
        }
        
        String title = issue.has("title") ? issue.get("title").asText() : "";
        String body = issue.has("body") ? issue.get("body").asText() : "";
        String combinedText = (title + " " + body).toLowerCase();
        
        // Look for dollar amounts in title or body
        BigDecimal amount = extractBountyAmount(combinedText);
        if (amount == null) {
            return null; // No bounty amount found
        }
        
        String issueId = issue.has("number") ? String.valueOf(issue.get("number").asInt()) : null;
        if (issueId == null) {
            return null;
        }
        
        String repositoryUrl = String.format("https://github.com/%s/%s", owner, repo);
        
        Bounty.BountyBuilder builder = Bounty.builder()
                .issueId(issueId)
                .repositoryUrl(repositoryUrl)
                .platform("github")
                .amount(amount)
                .currency("USD")
                .status(BountyStatus.OPEN);
        
        if (!title.isEmpty()) {
            builder.title(title);
        }
        
        if (!body.isEmpty()) {
            builder.description(body);
        }
        
        // Parse created date if available
        if (issue.has("created_at")) {
            try {
                String createdAtStr = issue.get("created_at").asText();
                builder.createdAt(java.time.LocalDateTime.parse(
                    createdAtStr.replace("Z", ""), 
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                ));
            } catch (Exception e) {
                log.debug("Failed to parse created_at for issue {}", issueId, e);
            }
        }
        
        return builder.build();
    }

    /**
     * Extract the first dollar amount found in the text.
     * Returns the highest amount if multiple are found.
     */
    private BigDecimal extractBountyAmount(String text) {
        Matcher matcher = BOUNTY_PATTERN.matcher(text);
        BigDecimal maxAmount = null;
        
        while (matcher.find()) {
            try {
                String amountStr = matcher.group(1).replace(",", "");
                BigDecimal amount = new BigDecimal(amountStr);
                
                // Only consider amounts >= $10 to filter out noise
                if (amount.compareTo(new BigDecimal("10")) >= 0) {
                    if (maxAmount == null || amount.compareTo(maxAmount) > 0) {
                        maxAmount = amount;
                    }
                }
            } catch (NumberFormatException e) {
                log.debug("Failed to parse bounty amount: {}", matcher.group(0));
            }
        }
        
        return maxAmount;
    }

    public Flux<Bounty> fetchBountiesFromRepositoryFallback(String owner, String repo, Throwable throwable) {
        log.error("GitHub API circuit breaker opened or rate limited for {}/{}", owner, repo, throwable);
        return Flux.empty();
    }
}

