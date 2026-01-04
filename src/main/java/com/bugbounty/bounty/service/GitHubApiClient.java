package com.bugbounty.bounty.service;

import com.bugbounty.bounty.domain.Bounty;
import reactor.core.publisher.Flux;

/**
 * Client for interacting with GitHub API to fetch issues with bounties.
 * Scans GitHub repositories for issues tagged with dollar amounts.
 */
public interface GitHubApiClient {
    
    /**
     * Fetch issues with bounties from a specific repository.
     * 
     * @param owner Repository owner (e.g., "facebook")
     * @param repo Repository name (e.g., "react")
     * @return Flux of Bounty objects representing issues with dollar amounts
     */
    Flux<Bounty> fetchBountiesFromRepository(String owner, String repo);
    
    /**
     * Fetch issues with bounties from multiple repositories.
     * 
     * @param repositories List of repository identifiers in format "owner/repo"
     * @return Flux of Bounty objects from all repositories
     */
    Flux<Bounty> fetchBountiesFromRepositories(java.util.List<String> repositories);
}

