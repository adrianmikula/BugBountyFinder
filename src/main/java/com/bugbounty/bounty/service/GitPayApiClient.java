package com.bugbounty.bounty.service;

import com.bugbounty.bounty.domain.Bounty;
import reactor.core.publisher.Flux;

/**
 * Client for interacting with GitPay.me API to fetch bounties.
 * GitPay is an open-source platform for completing tasks in exchange for bounties.
 * 
 * Reference: https://gitpay.me/
 */
public interface GitPayApiClient {
    
    /**
     * Fetch open bounties from GitPay.me.
     * 
     * @return Flux of Bounty objects representing available bounties
     */
    Flux<Bounty> fetchBounties();
}

