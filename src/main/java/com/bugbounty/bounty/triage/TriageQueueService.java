package com.bugbounty.bounty.triage;

import com.bugbounty.bounty.domain.Bounty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TriageQueueService {

    private static final String TRIAGE_QUEUE_KEY = "triage:queue";
    private static final double BASE_PRIORITY = 1000.0;

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void enqueue(Bounty bounty) {
        try {
            String bountyJson = objectMapper.writeValueAsString(bounty);
            double priority = calculatePriority(bounty);

            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            zSetOps.add(TRIAGE_QUEUE_KEY, bountyJson, priority);

            log.debug("Enqueued bounty {} with priority {}", bounty.getIssueId(), priority);
        } catch (Exception e) {
            log.error("Failed to enqueue bounty: {}", bounty.getIssueId(), e);
            throw new RuntimeException("Failed to enqueue bounty", e);
        }
    }

    public Bounty dequeue() {
        try {
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            Set<ZSetOperations.TypedTuple<String>> results = zSetOps.popMax(TRIAGE_QUEUE_KEY, 1);

            if (results == null || results.isEmpty()) {
                return null;
            }

            String bountyJson = results.iterator().next().getValue();
            Bounty bounty = objectMapper.readValue(bountyJson, Bounty.class);

            log.debug("Dequeued bounty: {}", bounty.getIssueId());
            return bounty;
        } catch (Exception e) {
            log.error("Failed to dequeue bounty", e);
            return null;
        }
    }

    public long getQueueSize() {
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
        Long size = zSetOps.zCard(TRIAGE_QUEUE_KEY);
        return size != null ? size : 0L;
    }

    public boolean isEmpty() {
        return getQueueSize() == 0;
    }

    public boolean remove(Bounty bounty) {
        try {
            String bountyJson = objectMapper.writeValueAsString(bounty);
            ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
            Long removed = zSetOps.remove(TRIAGE_QUEUE_KEY, bountyJson);
            return removed != null && removed > 0;
        } catch (Exception e) {
            log.error("Failed to remove bounty from queue: {}", bounty.getIssueId(), e);
            return false;
        }
    }

    private double calculatePriority(Bounty bounty) {
        double priority = BASE_PRIORITY;

        // Higher amount = higher priority
        if (bounty.getAmount() != null) {
            priority += bounty.getAmount().doubleValue();
        }

        // Platform-specific priority adjustments
        if ("algora".equals(bounty.getPlatform())) {
            priority += 100; // Slight preference for Algora
        }

        // Newer bounties get slight priority boost (if we track creation time)
        // This could be enhanced with timestamp-based scoring

        return priority;
    }
}

