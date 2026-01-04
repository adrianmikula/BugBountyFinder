package com.bugbounty.bounty.service;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.mapper.BountyMapper;
import com.bugbounty.bounty.repository.BountyRepository;
import com.bugbounty.bounty.triage.BountyFilteringService;
import com.bugbounty.bounty.triage.FilterResult;
import com.bugbounty.bounty.triage.TriageQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class BountyPollingService {

    private final AlgoraApiClient algoraApiClient;
    private final PolarApiClient polarApiClient;
    // Note: GitHubIssueScannerService is deprecated - bounties come from Algora/Polar.sh
    @SuppressWarnings("unused")
    private final GitHubIssueScannerService githubIssueScannerService;
    private final BountyRepository bountyRepository;
    private final BountyMapper bountyMapper;
    private final BountyFilteringService filteringService;
    private final TriageQueueService triageQueueService;

    private static final BigDecimal DEFAULT_MINIMUM_AMOUNT = new BigDecimal("50.00");
    
    @Value("${app.bounty.polling.enabled:true}")
    private boolean pollingEnabled;
    
    @Value("${app.bounty.polling.interval-seconds:300}")
    private long pollingIntervalSeconds;
    
    /**
     * Scheduled task to poll all platforms (Algora, Polar, GitHub) for new bounties.
     * Runs every 5 minutes by default (configurable via application.yml).
     * Note: fixedDelayString expects milliseconds, so we multiply seconds by 1000.
     */
    @Scheduled(fixedDelayString = "${app.bounty.polling.interval-seconds:300}000", initialDelay = 30000)
    public void scheduledPollAllPlatforms() {
        if (!pollingEnabled) {
            log.debug("Bounty polling is disabled");
            return;
        }
        
        log.info("Starting scheduled bounty polling from all platforms");
        pollAllPlatforms()
                .doOnComplete(() -> log.info("Scheduled bounty polling completed"))
                .doOnError(error -> log.error("Error during scheduled bounty polling", error))
                .subscribe();
    }

    public Flux<Bounty> pollAlgora() {
        return pollAlgora(DEFAULT_MINIMUM_AMOUNT);
    }

    public Flux<Bounty> pollAlgora(BigDecimal minimumAmount) {
        log.debug("Polling Algora API for new bounties");
        
        return algoraApiClient.fetchBounties()
                .filter(bounty -> !bountyRepository.existsByIssueIdAndPlatform(
                        bounty.getIssueId(), 
                        bounty.getPlatform()))
                .filter(bounty -> bounty.meetsMinimumAmount(minimumAmount))
                .flatMap(bounty -> {
                    // Save to database
                    log.info("Saving new bounty: {} from {}", bounty.getIssueId(), bounty.getPlatform());
                    var entity = bountyMapper.toEntity(bounty);
                    var saved = bountyRepository.save(entity);
                    Bounty savedBounty = bountyMapper.toDomain(saved);
                    
                    // Filter and enqueue for triage
                    FilterResult filterResult = filteringService.shouldProcess(savedBounty);
                    if (filterResult.shouldProcess()) {
                        log.info("Bounty {} passed filtering, enqueuing for triage", savedBounty.getIssueId());
                        triageQueueService.enqueue(savedBounty);
                    } else {
                        log.debug("Bounty {} filtered out: {}", savedBounty.getIssueId(), filterResult.reason());
                    }
                    
                    return Flux.just(savedBounty);
                })
                .doOnError(error -> log.error("Error polling Algora API", error));
    }

    public Flux<Bounty> pollPolar() {
        return pollPolar(DEFAULT_MINIMUM_AMOUNT);
    }

    public Flux<Bounty> pollPolar(BigDecimal minimumAmount) {
        log.debug("Polling Polar API for new bounties");
        
        return polarApiClient.fetchBounties()
                .filter(bounty -> !bountyRepository.existsByIssueIdAndPlatform(
                        bounty.getIssueId(), 
                        bounty.getPlatform()))
                .filter(bounty -> bounty.meetsMinimumAmount(minimumAmount))
                .flatMap(bounty -> {
                    // Save to database
                    log.info("Saving new bounty: {} from {}", bounty.getIssueId(), bounty.getPlatform());
                    var entity = bountyMapper.toEntity(bounty);
                    var saved = bountyRepository.save(entity);
                    Bounty savedBounty = bountyMapper.toDomain(saved);
                    
                    // Filter and enqueue for triage
                    FilterResult filterResult = filteringService.shouldProcess(savedBounty);
                    if (filterResult.shouldProcess()) {
                        log.info("Bounty {} passed filtering, enqueuing for triage", savedBounty.getIssueId());
                        triageQueueService.enqueue(savedBounty);
                    } else {
                        log.debug("Bounty {} filtered out: {}", savedBounty.getIssueId(), filterResult.reason());
                    }
                    
                    return Flux.just(savedBounty);
                })
                .doOnError(error -> log.error("Error polling Polar API", error));
    }

    public Flux<Bounty> pollAllPlatforms() {
        return pollAllPlatforms(DEFAULT_MINIMUM_AMOUNT);
    }

    public Flux<Bounty> pollAllPlatforms(BigDecimal minimumAmount) {
        log.debug("Polling all platforms for new bounties");
        
        // Note: Bounties are discovered via Algora and Polar.sh platforms
        // These platforms link bounties to GitHub issues
        // We do NOT scan GitHub issues directly for dollar amounts
        return Flux.merge(
                pollAlgora(minimumAmount),
                pollPolar(minimumAmount)
        );
    }
    
    /**
     * Poll GitHub repositories for issues with bounties.
     * 
     * @deprecated Bounties should be discovered via Algora/Polar.sh platforms, not by scanning GitHub.
     * This method is kept for backward compatibility but should not be used.
     */
    @Deprecated
    public Flux<Bounty> pollGitHub(BigDecimal minimumAmount) {
        log.warn("pollGitHub() is deprecated. Bounties should be discovered via Algora/Polar.sh platforms.");
        return Flux.empty(); // Disabled - use Algora/Polar.sh instead
    }
    
    @Deprecated
    public Flux<Bounty> pollGitHub() {
        return pollGitHub(DEFAULT_MINIMUM_AMOUNT);
    }
}

