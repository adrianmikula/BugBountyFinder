# System Integrations

## External API Integrations

### 1. Bounty Platforms

#### Algora API
- **Purpose**: Discover bug bounties
- **Endpoint**: `https://api.algora.io/v1/bounties`
- **Rate Limit**: 60 requests/minute
- **Resilience**: Circuit breaker + rate limiter
- **Client**: `AlgoraApiClient` / `AlgoraApiClientImpl`
- **Pattern**: Reactive (Flux<Bounty>)

#### Polar.sh API
- **Purpose**: Discover PR bounties
- **Endpoint**: `https://api.polar.sh/api/v1/bounties`
- **Rate Limit**: 60 requests/minute
- **Resilience**: Circuit breaker + rate limiter
- **Client**: `PolarApiClient` / `PolarApiClientImpl`
- **Pattern**: Reactive (Flux<Bounty>)

### 2. Security & Vulnerability

#### NVD (National Vulnerability Database)
- **Purpose**: Monitor for new CVEs
- **Endpoint**: `https://services.nvd.nist.gov/rest/json`
- **Rate Limit**: 5 requests per 30 seconds (6s delay between requests)
- **Client**: `NvdApiClient` / `NvdApiClientImpl`
- **Pattern**: Reactive (Flux<CVE>)
- **Filtering**: By severity (CRITICAL, HIGH, MEDIUM, LOW)

### 3. Version Control

#### GitHub
- **API**: Repository metadata and operations
- **Webhooks**: Real-time push event notifications
- **Rate Limit**: 5,000 requests/hour
- **Authentication**: Webhook signature verification
- **Operations**: Clone, pull, file reading via JGit

## Internal Service Integrations

### LLM Integration (Ollama)

**Architecture:**
- **Framework**: Spring AI (`ChatClient` abstraction)
- **Provider**: Ollama (local inference)
- **Model**: Configurable (default: `llama3.2:3b`)
- **Endpoint**: `http://localhost:11434`

**Usage Patterns:**

1. **Bounty Filtering** (`BountyFilteringService`)
   - Evaluates bounty viability
   - Returns confidence score and time estimate
   - JSON-structured responses

2. **Future: Code Analysis**
   - Issue triage and bug location
   - Code fix generation
   - PR description generation

**Benefits:**
- Cost-effective (local inference)
- Privacy (no data sent to external services)
- Low latency (no network overhead)
- Easy migration path to cloud LLMs via Spring AI abstraction

### Database Integrations

#### PostgreSQL
- **Purpose**: Persistent state storage
- **Schema Management**: Liquibase
- **Entities**: Bounty, CVE, Repository
- **Pattern**: JPA/Hibernate with domain-entity separation

#### Redis
- **Purpose**: Queue management and caching
- **Data Structures**:
  - **ZSet**: Priority queue for bounty triage (`triage:queue`)
  - **Future**: Cache for API responses and LLM results
- **Pattern**: Spring Data Redis

### Git Operations (JGit)

- **Abstraction**: `GitOperations` interface
- **Implementation**: `JGitOperations`
- **Operations**:
  - Clone repositories
  - Pull updates
  - Read files
  - List directory contents
- **Pattern**: Virtual threads for concurrent operations

## Integration Patterns

### 1. Reactive API Clients
- All external API calls use **WebFlux WebClient**
- Returns `Flux<T>` or `Mono<T>` for non-blocking operations
- Enables parallel processing and backpressure handling

### 2. Circuit Breaker Pattern
- **Resilience4j** circuit breakers protect against:
  - API failures
  - Timeouts
  - Cascading failures
- **Fallback**: Returns empty Flux on failure

### 3. Rate Limiting
- **Resilience4j** rate limiters enforce:
  - Per-API rate limits
  - Request throttling
  - API compliance

### 4. Webhook Security
- **HMAC-SHA256** signature verification
- Validates webhook authenticity
- Prevents unauthorized event processing

### 5. Scheduled Tasks
- **Spring Scheduling** for periodic operations:
  - Bounty polling (every 5 minutes)
  - CVE monitoring (every 1 hour)
- Configurable intervals via `application.yml`

## Data Flow Between Integrations

```
External APIs → API Clients → Services → Domain Models → Persistence
                                                              │
                                                              ▼
                                                         PostgreSQL/Redis
                                                              │
                                                              ▼
                                                         Services → LLM
                                                              │
                                                              ▼
                                                         Queue/Processing
```

**Key Characteristics:**
- **Asynchronous**: Reactive streams enable non-blocking I/O
- **Resilient**: Circuit breakers and rate limiters prevent failures
- **Scalable**: Virtual threads enable high concurrency
- **Observable**: Spring Actuator provides health and metrics

