package com.bugbounty.bounty.service;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.mapper.BountyMapper;
import com.bugbounty.bounty.repository.BountyRepository;
import com.bugbounty.bounty.triage.BountyFilteringService;
import com.bugbounty.bounty.triage.FilterResult;
import com.bugbounty.bounty.triage.TriageQueueService;
import com.bugbounty.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for processing GitHub issues that have bounties attached via Algora/Polar.sh.
 * 
 * NOTE: Bounties are NOT discovered by scanning GitHub issues for dollar amounts.
 * Instead, bounties are discovered by polling Algora and Polar.sh platforms, which
 * link to GitHub issues. This service processes those bounties.
 * 
 * This service is kept for backward compatibility but the primary flow is:
 * Algora/Polar.sh → Bounty with GitHub issue link → Issue analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubIssueScannerService {

    private final GitHubApiClient githubApiClient;
    private final BountyRepository bountyRepository;
    private final BountyMapper bountyMapper;
    private final BountyFilteringService filteringService;
    private final TriageQueueService triageQueueService;
    private final RepositoryRepository repositoryRepository;

    @Value("${app.bounty.github.minimum-amount:50.00}")
    private BigDecimal minimumAmount;

    @Value("${app.bounty.github.enabled:true}")
    private boolean enabled;

    /**
     * Scan all tracked repositories for issues with bounties.
     */
    public Flux<Bounty> scanTrackedRepositories() {
        if (!enabled) {
            log.debug("GitHub issue scanning is disabled");
            return Flux.empty();
        }

        log.info("Scanning tracked repositories for GitHub issues with bounties");
        
        // Get list of tracked repositories from database
        List<String> repositories = repositoryRepository.findAll().stream()
                .map(repo -> {
                    // Convert repository URL to owner/repo format
                    String url = repo.getUrl();
                    if (url.contains("github.com")) {
                        String[] parts = url.replace("https://github.com/", "")
                                           .replace("http://github.com/", "")
                                           .replace(".git", "")
                                           .split("/");
                        if (parts.length >= 2) {
                            return parts[0] + "/" + parts[1];
                        }
                    }
                    return null;
                })
                .filter(repo -> repo != null)
                .collect(Collectors.toList());

        if (repositories.isEmpty()) {
            log.debug("No tracked repositories found for GitHub issue scanning");
            return Flux.empty();
        }

        log.info("Scanning {} repositories for GitHub bounties", repositories.size());
        return scanRepositories(repositories);
    }

    /**
     * Scan specific repositories for issues with bounties.
     */
    public Flux<Bounty> scanRepositories(List<String> repositories) {
        return githubApiClient.fetchBountiesFromRepositories(repositories)
                .filter(bounty -> !bountyRepository.existsByIssueIdAndPlatform(
                        bounty.getIssueId(), 
                        bounty.getPlatform()))
                .filter(bounty -> bounty.meetsMinimumAmount(minimumAmount))
                .flatMap(bounty -> {
                    // Save to database
                    log.info("Found new GitHub bounty: {} in {}", bounty.getIssueId(), bounty.getRepositoryUrl());
                    var entity = bountyMapper.toEntity(bounty);
                    var saved = bountyRepository.save(entity);
                    Bounty savedBounty = bountyMapper.toDomain(saved);
                    
                    // Filter and enqueue for triage
                    FilterResult filterResult = filteringService.shouldProcess(savedBounty);
                    if (filterResult.shouldProcess()) {
                        log.info("GitHub bounty {} passed filtering, enqueuing for triage", savedBounty.getIssueId());
                        triageQueueService.enqueue(savedBounty);
                    } else {
                        log.debug("GitHub bounty {} filtered out: {}", savedBounty.getIssueId(), filterResult.reason());
                    }
                    
                    return Flux.just(savedBounty);
                })
                .doOnError(error -> log.error("Error scanning GitHub repositories", error));
    }
    
    /**
     * Process a single issue from a webhook event.
     * This is called in real-time when an issue is created or updated.
     * 
     * @param repositoryUrl The repository URL (e.g., "https://github.com/owner/repo")
     * @param issueNumber The issue number
     * @param issueTitle The issue title
     * @param issueBody The issue body/description
     * @return Bounty if a bounty amount was found, null otherwise
     */
    public Bounty processIssueFromWebhook(String repositoryUrl, String issueNumber, 
                                          String issueTitle, String issueBody) {
        if (!enabled) {
            log.debug("GitHub issue scanning is disabled");
            return null;
        }
        
        log.debug("Processing issue #{} from webhook in repository {}", issueNumber, repositoryUrl);
        
        // Extract bounty amount from title and body
        String combinedText = ((issueTitle != null ? issueTitle : "") + " " + 
                               (issueBody != null ? issueBody : "")).toLowerCase();
        
        BigDecimal amount = extractBountyAmount(combinedText);
        if (amount == null || amount.compareTo(minimumAmount) < 0) {
            log.debug("No bounty amount found in issue #{} or amount below minimum", issueNumber);
            return null;
        }
        
        // Check if bounty already exists
        if (bountyRepository.existsByIssueIdAndPlatform(issueNumber, "github")) {
            log.debug("Bounty for issue #{} already exists", issueNumber);
            return null;
        }
        
        // Create bounty object
        Bounty bounty = Bounty.builder()
                .issueId(issueNumber)
                .repositoryUrl(repositoryUrl)
                .platform("github")
                .amount(amount)
                .currency("USD")
                .title(issueTitle)
                .description(issueBody)
                .status(com.bugbounty.bounty.domain.BountyStatus.OPEN)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        
        // Save to database
        log.info("Found new GitHub bounty from webhook: Issue #{} in {} (${})", 
                issueNumber, repositoryUrl, amount);
        var entity = bountyMapper.toEntity(bounty);
        var saved = bountyRepository.save(entity);
        Bounty savedBounty = bountyMapper.toDomain(saved);
        
        // Filter and enqueue for triage
        FilterResult filterResult = filteringService.shouldProcess(savedBounty);
        if (filterResult.shouldProcess()) {
            log.info("GitHub bounty #{} passed filtering, enqueuing for triage", issueNumber);
            triageQueueService.enqueue(savedBounty);
        } else {
            log.debug("GitHub bounty #{} filtered out: {}", issueNumber, filterResult.reason());
        }
        
        return savedBounty;
    }
    
    /**
     * Extract the first dollar amount found in the text.
     * Returns the highest amount if multiple are found.
     */
    private BigDecimal extractBountyAmount(String text) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\\$([0-9]{1,3}(?:,?[0-9]{3})*(?:\\.[0-9]{2})?)", 
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);
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
}

