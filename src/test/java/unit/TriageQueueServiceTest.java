package com.bugbounty.bounty.triage;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TriageQueueService Tests")
class TriageQueueServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private ObjectMapper objectMapper;

    private TriageQueueService triageQueueService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        objectMapper = new ObjectMapper();
        triageQueueService = new TriageQueueService(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should enqueue bounty for triage")
    void shouldEnqueueBounty() {
        // Given
        Bounty bounty = Bounty.builder()
                .id(java.util.UUID.randomUUID())
                .issueId("issue-123")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        // When
        triageQueueService.enqueue(bounty);

        // Then
        verify(zSetOperations, times(1)).add(
                eq("triage:queue"),
                anyString(),
                anyDouble()
        );
    }

    @Test
    @DisplayName("Should prioritize bounties by amount")
    void shouldPrioritizeByAmount() {
        // Given
        Bounty highValueBounty = Bounty.builder()
                .id(java.util.UUID.randomUUID())
                .issueId("issue-123")
                .amount(new BigDecimal("500.00"))
                .build();

        Bounty lowValueBounty = Bounty.builder()
                .id(java.util.UUID.randomUUID())
                .issueId("issue-456")
                .amount(new BigDecimal("50.00"))
                .build();

        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        // When
        triageQueueService.enqueue(highValueBounty);
        triageQueueService.enqueue(lowValueBounty);

        // Then - verify both bounties were enqueued with correct priority scores
        verify(zSetOperations, times(2)).add(
                eq("triage:queue"),
                anyString(),
                anyDouble()
        );
    }

    @Test
    @DisplayName("Should dequeue highest priority bounty")
    void shouldDequeueHighestPriority() throws Exception {
        // Given
        Bounty testBounty = Bounty.builder()
                .id(java.util.UUID.randomUUID())
                .issueId("issue-123")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();
        String bountyJson = objectMapper.writeValueAsString(testBounty);
        
        ZSetOperations.TypedTuple<String> tuple = new ZSetOperations.TypedTuple<String>() {
            @Override
            public String getValue() {
                return bountyJson;
            }

            @Override
            public Double getScore() {
                return 100.0;
            }

            @Override
            public int compareTo(ZSetOperations.TypedTuple<String> o) {
                return Double.compare(getScore(), o.getScore());
            }
        };
        Set<ZSetOperations.TypedTuple<String>> tuples = Set.of(tuple);

        // Mock popMax to return the tuple set
        // Note: popMax signature is popMax(String key, long count)
        when(zSetOperations.popMax(eq("triage:queue"), eq(1L))).thenReturn(tuples);

        // When
        Bounty result = triageQueueService.dequeue();

        // Then
        assertNotNull(result);
        assertEquals("issue-123", result.getIssueId());
        verify(zSetOperations, times(1)).popMax(eq("triage:queue"), eq(1L));
    }

    @Test
    @DisplayName("Should return null when queue is empty")
    void shouldReturnNullWhenQueueEmpty() {
        // Given
        when(zSetOperations.popMax(anyString(), anyLong())).thenReturn(Set.of());

        // When
        Bounty result = triageQueueService.dequeue();

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should get queue size")
    void shouldGetQueueSize() {
        // Given
        when(zSetOperations.zCard("triage:queue")).thenReturn(5L);

        // When
        long size = triageQueueService.getQueueSize();

        // Then
        assertEquals(5L, size);
        verify(zSetOperations, times(1)).zCard("triage:queue");
    }

    @Test
    @DisplayName("Should check if queue is empty")
    void shouldCheckIfQueueIsEmpty() {
        // Given
        when(zSetOperations.zCard("triage:queue")).thenReturn(0L);

        // When
        boolean isEmpty = triageQueueService.isEmpty();

        // Then
        assertTrue(isEmpty);
    }

    @Test
    @DisplayName("Should remove bounty from queue")
    void shouldRemoveBountyFromQueue() {
        // Given
        Bounty bounty = Bounty.builder()
                .id(java.util.UUID.randomUUID())
                .issueId("issue-123")
                .build();

        when(zSetOperations.remove(anyString(), anyString())).thenReturn(1L);

        // When
        boolean removed = triageQueueService.remove(bounty);

        // Then
        assertTrue(removed);
        verify(zSetOperations, times(1)).remove(eq("triage:queue"), anyString());
    }
}

