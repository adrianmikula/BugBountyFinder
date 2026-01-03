package com.bugbounty.bounty.service;

import com.bugbounty.bounty.domain.Bounty;
import com.bugbounty.bounty.domain.BountyStatus;
import com.bugbounty.bounty.entity.BountyEntity;
import com.bugbounty.bounty.mapper.BountyMapper;
import com.bugbounty.bounty.repository.BountyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BountyPollingService Tests")
class BountyPollingServiceTest {

    @Mock
    private AlgoraApiClient algoraApiClient;

    @Mock
    private PolarApiClient polarApiClient;

    @Mock
    private BountyRepository bountyRepository;

    @Mock
    private BountyMapper bountyMapper;

    @Mock
    private com.bugbounty.bounty.triage.BountyFilteringService filteringService;

    @Mock
    private com.bugbounty.bounty.triage.TriageQueueService triageQueueService;

    @InjectMocks
    private BountyPollingService bountyPollingService;

    @BeforeEach
    void setUp() {
        // Setup common test configuration
    }

    @Test
    @DisplayName("Should poll Algora API and save new bounties")
    void shouldPollAlgoraAndSaveNewBounties() {
        // Given
        Bounty newBounty = Bounty.builder()
                .issueId("algora-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        BountyEntity entity = BountyEntity.builder()
                .issueId("algora-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        when(algoraApiClient.fetchBounties()).thenReturn(Flux.just(newBounty));
        when(bountyRepository.existsByIssueIdAndPlatform(anyString(), anyString())).thenReturn(false);
        when(bountyMapper.toEntity(any(Bounty.class))).thenReturn(entity);
        when(bountyRepository.save(any(BountyEntity.class))).thenReturn(entity);
        when(bountyMapper.toDomain(any(BountyEntity.class))).thenReturn(newBounty);
        when(filteringService.shouldProcess(any(Bounty.class)))
                .thenReturn(new com.bugbounty.bounty.triage.FilterResult(true, 0.8, 30, "Good candidate"));

        // When
        List<Bounty> result = bountyPollingService.pollAlgora().collectList().block();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bountyRepository, times(1)).save(any(BountyEntity.class));
        verify(filteringService, times(1)).shouldProcess(any(Bounty.class));
        verify(triageQueueService, times(1)).enqueue(any(Bounty.class));
    }

    @Test
    @DisplayName("Should skip bounties that already exist")
    void shouldSkipExistingBounties() {
        // Given
        Bounty existingBounty = Bounty.builder()
                .issueId("algora-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        when(algoraApiClient.fetchBounties()).thenReturn(Flux.just(existingBounty));
        when(bountyRepository.existsByIssueIdAndPlatform("algora-123", "algora")).thenReturn(true);

        // When
        List<Bounty> result = bountyPollingService.pollAlgora().collectList().block();

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
        verify(bountyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should filter bounties by minimum amount")
    void shouldFilterByMinimumAmount() {
        // Given
        Bounty highValueBounty = Bounty.builder()
                .issueId("algora-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        Bounty lowValueBounty = Bounty.builder()
                .issueId("algora-456")
                .repositoryUrl("https://github.com/owner/repo2")
                .platform("algora")
                .amount(new BigDecimal("25.00"))
                .status(BountyStatus.OPEN)
                .build();

        when(algoraApiClient.fetchBounties()).thenReturn(Flux.just(highValueBounty, lowValueBounty));
        when(bountyRepository.existsByIssueIdAndPlatform(anyString(), anyString())).thenReturn(false);
        when(bountyMapper.toEntity(any(Bounty.class))).thenAnswer(invocation -> {
            Bounty b = invocation.getArgument(0);
            return BountyEntity.builder()
                    .issueId(b.getIssueId())
                    .repositoryUrl(b.getRepositoryUrl())
                    .platform(b.getPlatform())
                    .amount(b.getAmount())
                    .status(b.getStatus())
                    .build();
        });
        when(bountyRepository.save(any(BountyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bountyMapper.toDomain(any(BountyEntity.class))).thenAnswer(invocation -> {
            BountyEntity e = invocation.getArgument(0);
            return Bounty.builder()
                    .issueId(e.getIssueId())
                    .repositoryUrl(e.getRepositoryUrl())
                    .platform(e.getPlatform())
                    .amount(e.getAmount())
                    .status(e.getStatus())
                    .build();
        });
        when(filteringService.shouldProcess(any(Bounty.class)))
                .thenReturn(new com.bugbounty.bounty.triage.FilterResult(true, 0.8, 30, "Good candidate"));

        // When
        List<Bounty> result = bountyPollingService.pollAlgora(new BigDecimal("50.00"))
                .collectList()
                .block();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("algora-123", result.get(0).getIssueId());
    }

    @Test
    @DisplayName("Should handle API errors gracefully")
    void shouldHandleApiErrors() {
        // Given
        when(algoraApiClient.fetchBounties())
                .thenReturn(Flux.error(new RuntimeException("API Error")));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            bountyPollingService.pollAlgora().blockFirst();
        });
    }

    @Test
    @DisplayName("Should poll Polar API and save new bounties")
    void shouldPollPolarAndSaveNewBounties() {
        // Given
        Bounty newBounty = Bounty.builder()
                .issueId("polar-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("polar")
                .amount(new BigDecimal("200.00"))
                .status(BountyStatus.OPEN)
                .build();

        BountyEntity entity = BountyEntity.builder()
                .issueId("polar-123")
                .repositoryUrl("https://github.com/owner/repo")
                .platform("polar")
                .amount(new BigDecimal("200.00"))
                .status(BountyStatus.OPEN)
                .build();

        when(polarApiClient.fetchBounties()).thenReturn(Flux.just(newBounty));
        when(bountyRepository.existsByIssueIdAndPlatform(anyString(), anyString())).thenReturn(false);
        when(bountyMapper.toEntity(any(Bounty.class))).thenReturn(entity);
        when(bountyRepository.save(any(BountyEntity.class))).thenReturn(entity);
        when(bountyMapper.toDomain(any(BountyEntity.class))).thenReturn(newBounty);
        when(filteringService.shouldProcess(any(Bounty.class)))
                .thenReturn(new com.bugbounty.bounty.triage.FilterResult(true, 0.8, 30, "Good candidate"));

        // When
        List<Bounty> result = bountyPollingService.pollPolar().collectList().block();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bountyRepository, times(1)).save(any(BountyEntity.class));
    }

    @Test
    @DisplayName("Should poll all platforms and aggregate results")
    void shouldPollAllPlatforms() {
        // Given
        Bounty algoraBounty = Bounty.builder()
                .issueId("algora-123")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        Bounty polarBounty = Bounty.builder()
                .issueId("polar-123")
                .platform("polar")
                .amount(new BigDecimal("200.00"))
                .status(BountyStatus.OPEN)
                .build();

        BountyEntity algoraEntity = BountyEntity.builder()
                .issueId("algora-123")
                .platform("algora")
                .amount(new BigDecimal("150.00"))
                .status(BountyStatus.OPEN)
                .build();

        BountyEntity polarEntity = BountyEntity.builder()
                .issueId("polar-123")
                .platform("polar")
                .amount(new BigDecimal("200.00"))
                .status(BountyStatus.OPEN)
                .build();

        when(algoraApiClient.fetchBounties()).thenReturn(Flux.just(algoraBounty));
        when(polarApiClient.fetchBounties()).thenReturn(Flux.just(polarBounty));
        when(bountyRepository.existsByIssueIdAndPlatform(anyString(), anyString())).thenReturn(false);
        when(bountyMapper.toEntity(algoraBounty)).thenReturn(algoraEntity);
        when(bountyMapper.toEntity(polarBounty)).thenReturn(polarEntity);
        when(bountyRepository.save(any(BountyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bountyMapper.toDomain(any(BountyEntity.class))).thenAnswer(invocation -> {
            BountyEntity e = invocation.getArgument(0);
            return e.getPlatform().equals("algora") ? algoraBounty : polarBounty;
        });
        when(filteringService.shouldProcess(any(Bounty.class)))
                .thenReturn(new com.bugbounty.bounty.triage.FilterResult(true, 0.8, 30, "Good candidate"));

        // When
        List<Bounty> result = bountyPollingService.pollAllPlatforms().collectList().block();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(bountyRepository, times(2)).save(any(BountyEntity.class));
    }
}

