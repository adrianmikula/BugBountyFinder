# Component Architecture

## Domain Layer

### Bounty Domain (`com.bugbounty.bounty`)

**Domain Model:**
- `Bounty`: Core domain object representing a bug bounty
- `BountyStatus`: Enum for bounty lifecycle states

**Services:**
- `BountyPollingService`: Orchestrates polling from multiple platforms
- `BountyFilteringService`: LLM-powered bounty evaluation
- `TriageQueueService`: Priority queue management for bounty processing

**API Clients:**
- `AlgoraApiClient`: Interface for Algora platform
- `PolarApiClient`: Interface for Polar.sh platform
- Implementations use WebClient with circuit breakers and rate limiters

**Persistence:**
- `BountyEntity`: JPA entity (separate from domain model)
- `BountyRepository`: Spring Data JPA repository
- `BountyMapper`: Converts between domain and entity

### CVE Domain (`com.bugbounty.cve`)

**Domain Model:**
- `CVE`: Represents a Common Vulnerability and Exposure

**Services:**
- `CVEMonitoringService`: Scheduled polling and webhook handling
- `RepositoryScanningService`: Scans repositories for vulnerable dependencies
- `LanguageMappingService`: Maps CVEs to programming languages

**API Client:**
- `NvdApiClient`: Interface for NVD API
- `NvdApiClientImpl`: Implementation with rate limiting

**Persistence:**
- `CVEEntity`: JPA entity
- `CVERepository`: Spring Data JPA repository
- `CVEMapper`: Domain-entity conversion

### Repository Domain (`com.bugbounty.repository`)

**Domain Model:**
- `Repository`: Represents a Git repository

**Services:**
- `RepositoryService`: High-level repository operations
  - Clone repositories
  - Update (pull) repositories
  - Read files and list directories

**Infrastructure:**
- `GitOperations`: Abstraction for Git operations
- `JGitOperations`: JGit-based implementation

**Persistence:**
- `RepositoryEntity`: JPA entity
- `RepositoryRepository`: Spring Data JPA repository

### Webhook Domain (`com.bugbounty.webhook`)

**DTOs:**
- `GitHubPushEvent`: GitHub webhook payload structure

**Services:**
- `GitHubWebhookService`: Processes GitHub push events
- `WebhookSignatureService`: Validates webhook signatures (HMAC-SHA256)

**Controllers:**
- `GitHubWebhookController`: REST endpoint for GitHub webhooks
- `CVEWebhookController`: REST endpoint for CVE notifications (future)

## Infrastructure Layer

### Configuration

**Redis Configuration** (`RedisConfig`)
- Configures Redis connection and template
- Sets up serialization for queue operations

**WebClient Configuration** (`WebClientConfig`)
- Creates platform-specific WebClient instances
- Configures timeouts and connection pools
- Separate clients for Algora, Polar, and NVD

### Resilience

**Circuit Breakers** (Resilience4j)
- `algoraApi`: Protects Algora API calls
- `polarApi`: Protects Polar API calls
- Configuration: sliding window, failure thresholds, half-open state

**Rate Limiters** (Resilience4j)
- `algoraApi`: 60 requests/minute
- `polarApi`: 60 requests/minute
- Prevents API quota exhaustion

### LLM Integration

**Spring AI Configuration**
- `ChatClient`: Abstraction for LLM interactions
- Ollama provider configured via `application.yml`
- Model and temperature settings configurable

**Usage:**
- `BountyFilteringService` uses `ChatClient` for bounty evaluation
- Structured prompts with JSON response parsing
- Error handling with fail-safe defaults

## Application Layer

### Main Application
- `BugBountyFinderApplication`: Spring Boot entry point
- Enables scheduling (`@EnableScheduling`)
- Enables async processing (`@EnableAsync`)

### Scheduled Tasks
- `BountyPollingService.pollAllPlatforms()`: Scheduled bounty discovery
- `CVEMonitoringService.pollForNewCVEs()`: Scheduled CVE monitoring

## Component Interactions

### Bounty Discovery Pipeline

```
Scheduler
    ↓
BountyPollingService
    ├─→ AlgoraApiClient (WebClient)
    ├─→ PolarApiClient (WebClient)
    ↓
BountyRepository (JPA)
    ↓
BountyFilteringService
    ├─→ ChatClient (Spring AI → Ollama)
    ↓
TriageQueueService
    └─→ Redis (ZSet)
```

### CVE Monitoring Pipeline

```
Scheduler
    ↓
CVEMonitoringService
    ├─→ NvdApiClient (WebClient)
    ├─→ CVERepository (JPA)
    ├─→ LanguageMappingService
    ├─→ RepositoryRepository (JPA)
    ↓
RepositoryScanningService
    ├─→ RepositoryService
    │   └─→ JGitOperations
    └─→ File System (Dependency Scanning)
```

### Webhook Processing Pipeline

```
GitHub Webhook
    ↓
GitHubWebhookController
    ├─→ WebhookSignatureService
    ↓
GitHubWebhookService
    ├─→ RepositoryService
    │   └─→ JGitOperations
    └─→ RepositoryRepository (JPA)
```

## Design Principles

1. **Separation of Concerns**
   - Domain models independent of persistence
   - API clients isolated from business logic
   - Infrastructure concerns separated from domain

2. **Dependency Inversion**
   - Interfaces for external dependencies (API clients, Git operations)
   - Easy to mock for testing
   - Flexible implementation swapping

3. **Reactive Programming**
   - Non-blocking I/O throughout
   - Backpressure handling
   - Efficient resource utilization

4. **Fault Tolerance**
   - Circuit breakers prevent cascading failures
   - Rate limiters protect API quotas
   - Graceful degradation on errors

5. **Testability**
   - Component tests with TestContainers
   - Unit tests with mocks
   - Clear boundaries for testing

