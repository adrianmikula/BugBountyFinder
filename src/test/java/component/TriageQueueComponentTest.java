package com.bugbounty.component;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.triage.TriageQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Component test for TriageQueueService.
 * Tests Redis queue operations in a real container environment.
 */
@DisplayName("Triage Queue Component Tests")
class TriageQueueComponentTest extends AbstractComponentTest {

    @Autowired
    private TriageQueueService triageQueueService;

    @BeforeEach
    void setUp() {
        // Clear queue before each test
        while (triageQueueService.dequeue() != null) {
            // Empty queue
        }
    }

    @Test
    @DisplayName("Should enqueue and dequeue bounties with priority ordering")
    void shouldEnqueueAndDequeueWithPriority() {
        // Given
        Bounty highValueBounty = Bounty.builder()
                .issueId("high-value-1")
                .repositoryUrl("https://github.com/test/repo")
                .platform("algora")
                .amount(new BigDecimal("500.00"))
                .status(BountyStatus.OPEN)
                .build();

        Bounty lowValueBounty = Bounty.builder()
                .issueId("low-value-1")
                .repositoryUrl("https://github.com/test/repo2")
                .platform("polar")
                .amount(new BigDecimal("50.00"))
                .status(BountyStatus.OPEN)
                .build();

        // When
        triageQueueService.enqueue(lowValueBounty);
        triageQueueService.enqueue(highValueBounty);

        // Then - Higher value should be dequeued first
        Bounty first = triageQueueService.dequeue();
        assertNotNull(first);
        assertEquals("high-value-1", first.getIssueId());

        Bounty second = triageQueueService.dequeue();
        assertNotNull(second);
        assertEquals("low-value-1", second.getIssueId());
    }

    @Test
    @DisplayName("Should track queue size correctly")
    void shouldTrackQueueSize() {
        // Given
        assertEquals(0, triageQueueService.getQueueSize());
        assertTrue(triageQueueService.isEmpty());

        Bounty bounty1 = Bounty.builder()
                .issueId("size-test-1")
                .repositoryUrl("https://github.com/test/repo")
                .platform("algora")
                .amount(new BigDecimal("100.00"))
                .status(BountyStatus.OPEN)
                .build();

        Bounty bounty2 = Bounty.builder()
                .issueId("size-test-2")
                .repositoryUrl("https://github.com/test/repo")
                .platform("algora")
                .amount(new BigDecimal("200.00"))
                .status(BountyStatus.OPEN)
                .build();

        // When
        triageQueueService.enqueue(bounty1);
        assertEquals(1, triageQueueService.getQueueSize());
        assertFalse(triageQueueService.isEmpty());

        triageQueueService.enqueue(bounty2);
        assertEquals(2, triageQueueService.getQueueSize());

        triageQueueService.dequeue();
        assertEquals(1, triageQueueService.getQueueSize());

        triageQueueService.dequeue();
        assertEquals(0, triageQueueService.getQueueSize());
        assertTrue(triageQueueService.isEmpty());
    }

    @Test
    @DisplayName("Should handle empty queue gracefully")
    void shouldHandleEmptyQueue() {
        // When
        Bounty result = triageQueueService.dequeue();

        // Then
        assertNull(result);
        assertTrue(triageQueueService.isEmpty());
        assertEquals(0, triageQueueService.getQueueSize());
    }

    @Test
    @DisplayName("Should remove specific bounty from queue")
    void shouldRemoveBountyFromQueue() {
        // Given
        Bounty bounty1 = Bounty.builder()
                .issueId("remove-test-1")
                .repositoryUrl("https://github.com/test/repo")
                .platform("algora")
                .amount(new BigDecimal("100.00"))
                .status(BountyStatus.OPEN)
                .build();

        Bounty bounty2 = Bounty.builder()
                .issueId("remove-test-2")
                .repositoryUrl("https://github.com/test/repo")
                .platform("algora")
                .amount(new BigDecimal("200.00"))
                .status(BountyStatus.OPEN)
                .build();

        triageQueueService.enqueue(bounty1);
        triageQueueService.enqueue(bounty2);

        // When
        boolean removed = triageQueueService.remove(bounty1);

        // Then
        assertTrue(removed);
        assertEquals(1, triageQueueService.getQueueSize());
        
        Bounty remaining = triageQueueService.dequeue();
        assertEquals("remove-test-2", remaining.getIssueId());
    }
}

