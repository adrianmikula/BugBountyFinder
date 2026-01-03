package com.bugbounty.bounty.triage;

public record FilterResult(
        boolean shouldProcess,
        double confidence,
        int estimatedTimeMinutes,
        String reason
) {
}

