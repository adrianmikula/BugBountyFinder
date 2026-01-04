# GitHub Issue Scanning Architecture

## Overview

The GitHub Issue Scanning feature implements the **"Bounty-per-Issue"** model from the requirements. This system scans GitHub repositories for issues that are tagged with dollar amounts (e.g., "$50 for fixing this React hydration error").

## Components

### 1. GitHubApiClient

**Interface:** `com.bugbounty.bounty.service.GitHubApiClient`

**Implementation:** `com.bugbounty.bounty.service.impl.GitHubApiClientImpl`

**Responsibilities:**
- Fetches open issues from GitHub repositories via the GitHub REST API
- Parses issue titles and bodies to extract dollar amounts
- Filters out pull requests (GitHub API returns both issues and PRs)
- Handles pagination for repositories with many issues
- Implements rate limiting and circuit breaker patterns

**Key Features:**
- Pattern matching for dollar amounts: `\$([0-9]{1,3}(?:,?[0-9]{3})*(?:\.[0-9]{2})?)`
- Minimum amount filtering (default: $10 to filter out noise)
- Returns the highest amount if multiple are found in an issue
- Supports scanning multiple repositories concurrently

### 2. GitHubIssueScannerService

**Location:** `com.bugbounty.bounty.service.GitHubIssueScannerService`

**Responsibilities:**
- Orchestrates scanning of tracked repositories
- Converts repository URLs to owner/repo format
- Filters bounties by minimum amount
- Saves discovered bounties to the database
- Integrates with triage queue for processing

**Configuration:**
- `app.bounty.github.enabled`: Enable/disable GitHub scanning (default: true)
- `app.bounty.github.minimum-amount`: Minimum bounty amount to consider (default: $50.00)

### 3. Integration with BountyPollingService

The `BountyPollingService` now includes GitHub scanning alongside Algora and Polar:

```java
public Flux<Bounty> pollAllPlatforms(BigDecimal minimumAmount) {
    return Flux.merge(
        pollAlgora(minimumAmount),
        pollPolar(minimumAmount),
        pollGitHub(minimumAmount)  // New GitHub scanning
    );
}
```

## Data Flow

### Real-Time Webhook Flow (Primary)

```
GitHub Issue Created/Reopened
    ↓
GitHub Webhook → GitHubWebhookController
    ↓
Verify Signature (HMAC-SHA256)
    ↓
GitHubWebhookService.processIssueEvent()
    ↓
GitHubIssueScannerService.processIssueFromWebhook()
    ↓
Extract Bounty Amount (regex pattern matching)
    ↓
Filter by minimum amount
    ↓
BountyRepository.save() → Save to database
    ↓
BountyFilteringService.shouldProcess() → LLM triage
    ↓
TriageQueueService.enqueue() → Add to processing queue
```

### Polling Flow (Fallback)

```
Scheduler (every 5 minutes)
    ↓
BountyPollingService.pollAllPlatforms()
    ↓
GitHubIssueScannerService.scanTrackedRepositories()
    ↓
RepositoryRepository.findAll() → Get tracked repos
    ↓
GitHubApiClient.fetchBountiesFromRepositories()
    ↓
GitHub REST API → Fetch issues
    ↓
Parse issues → Extract dollar amounts
    ↓
Filter by minimum amount
    ↓
BountyRepository.save() → Save to database
    ↓
BountyFilteringService.shouldProcess() → LLM triage
    ↓
TriageQueueService.enqueue() → Add to processing queue
```

**Note:** Webhooks provide real-time detection, while polling serves as a fallback for reliability.

## Rate Limiting

GitHub API rate limits:
- **Unauthenticated:** 60 requests/hour per IP
- **Authenticated:** 5,000 requests/hour per token

**Configuration:**
- Rate limiter: 80 requests/minute (conservative limit)
- Circuit breaker: Opens on 50% failure rate
- Retry logic: Exponential backoff for 5xx errors and rate limits (429)

## Pattern Matching

The system uses regex to find dollar amounts in issue titles and bodies:

**Pattern:** `\$([0-9]{1,3}(?:,?[0-9]{3})*(?:\.[0-9]{2})?)`

**Examples:**
- "$50 for fixing this bug" → $50.00
- "Offering $100 to fix the hydration error" → $100.00
- "$1,000 bounty for security fix" → $1,000.00
- "Bounty: $500.00" → $500.00

**Filtering:**
- Only amounts >= $10 are considered (to filter out noise)
- If multiple amounts found, the highest is used

## Configuration

### application.yml

```yaml
app:
  bounty:
    polling:
      interval-seconds: 300  # 5 minutes
      enabled: true
    platforms:
      github:
        api-url: https://api.github.com
        api-token: ${GITHUB_API_TOKEN:}  # Optional but recommended
        rate-limit-per-hour: 5000
        enabled: true
        minimum-amount: 50.00
```

### Resilience4j

```yaml
resilience4j:
  circuitbreaker:
    instances:
      githubApi:
        slidingWindowSize: 10
        failureRateThreshold: 50
  ratelimiter:
    instances:
      githubApi:
        limitForPeriod: 80
        limitRefreshPeriod: 60s
```

## Webhook Integration

The system supports **real-time webhook notifications** from GitHub:

- **Endpoint:** `/api/webhooks/github` or `/api/webhooks/github/issues`
- **Events:** `issues` (opened, reopened)
- **Security:** HMAC-SHA256 signature verification
- **Benefits:** Real-time detection, no polling delay, more efficient

See [GitHub Issue Webhook Setup](../setup/GITHUB_ISSUE_WEBHOOK_SETUP.md) for configuration details.

## Future Enhancements

1. **Issue Label Support:** Scan for issues with specific labels (e.g., "bounty", "reward")
2. **Comment Scanning:** Also scan issue comments for bounty amounts
3. **Issue Edited Events:** Process when issues are edited (currently only opened/reopened)
4. **Repository Discovery:** Automatically discover popular repositories to monitor
5. **Bounty Amount Validation:** Use LLM to validate if amounts are real bounties vs. examples

## Notes

- The CVE monitoring modules are kept in place for future use when implementing the CVE scanning against actual websites (using tools like nuclei)
- This GitHub issue scanning focuses on the "Bounty-per-Issue" model, which is the first priority per requirements

