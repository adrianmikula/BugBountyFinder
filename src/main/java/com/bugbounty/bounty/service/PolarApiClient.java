package com.bugbounty.bounty.service;

import com.bugbounty.bounty.domain.Bounty;
import reactor.core.publisher.Flux;

public interface PolarApiClient {
    Flux<Bounty> fetchBounties();
}

