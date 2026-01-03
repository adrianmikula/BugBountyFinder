# Component Tests

This document describes the component testing strategy using Spring Boot Test with TestContainers.

## Overview

Component tests verify that major features work correctly in an integrated environment with real containers (PostgreSQL, Redis). This is similar to Arquillian's approach but uses Spring Boot Test, which is the standard for Spring Boot applications.

## Test Infrastructure

### Base Test Class

All component tests extend `AbstractComponentTest`, which provides:

- **TestContainers Setup**: PostgreSQL and Redis containers
- **Spring Boot Test**: Full application context loading
- **Dynamic Properties**: Automatic configuration of database and Redis connections
- **Profile**: Uses `component-test` profile for test-specific configuration

### Test Configuration

- **Profile**: `component-test`
- **Configuration File**: `application-component-test.yml`
- **Containers**: 
  - PostgreSQL 15 (alpine)
  - Redis 7 (alpine)
- **Container Reuse**: Enabled for faster test execution

## Component Test Suite

### 1. BountyPollingComponentTest

Tests the complete bounty polling workflow:

- Polling APIs (Algora, Polar)
- Saving to database
- Filtering with LLM
- Enqueuing to triage queue
- Duplicate detection
- Minimum amount filtering

**Key Assertions:**
- Bounties are persisted correctly
- Filtering service is called
- Accepted bounties are enqueued
- Duplicates are rejected

### 2. TriageQueueComponentTest

Tests Redis-based priority queue:

- Enqueue/dequeue operations
- Priority ordering (higher value = higher priority)
- Queue size tracking
- Empty queue handling
- Bounty removal from queue

**Key Assertions:**
- Higher value bounties dequeued first
- Queue size tracked correctly
- Operations are atomic

### 3. BountyFilteringComponentTest

Tests LLM-based filtering service:

- High-value bounty acceptance
- Low-value/complex bounty rejection
- Confidence threshold application
- Time threshold application
- Error handling

**Key Assertions:**
- LLM responses are parsed correctly
- Thresholds are applied
- Fail-safe behavior on errors

### 4. RepositoryServiceComponentTest

Tests Git repository operations:

- Repository cloning
- URL parsing (various formats)
- Clone status checking
- Owner/name extraction

**Key Assertions:**
- Repositories clone successfully
- URL formats are handled correctly
- Status tracking works

### 5. ApiClientComponentTest

Tests HTTP client integration:

- Algora API client
- Polar API client
- Error handling
- Rate limiting

**Key Assertions:**
- API responses are parsed correctly
- Errors are handled gracefully
- Rate limits are respected

## Running Component Tests

### Prerequisites

- Docker must be running
- TestContainers will automatically pull and start containers

### Commands

```bash
# Run all component tests
./gradlew test --tests "com.bugbounty.component.*"

# Run specific test class
./gradlew test --tests "com.bugbounty.component.BountyPollingComponentTest"

# Run with verbose output
./gradlew test --tests "com.bugbounty.component.*" --info
```

### Test Execution

1. TestContainers starts PostgreSQL and Redis containers
2. Spring Boot Test loads full application context
3. Dynamic properties configure connections to containers
4. Tests execute against real containers
5. Containers are cleaned up after tests

## Best Practices

1. **Isolation**: Each test should be independent
2. **Cleanup**: Use `@BeforeEach` to reset state
3. **Mocking**: Mock external services (LLM, APIs) when appropriate
4. **Assertions**: Use AssertJ for fluent assertions
5. **Naming**: Use descriptive test names with `@DisplayName`

## Troubleshooting

### Containers Not Starting

- Ensure Docker is running
- Check Docker daemon is accessible
- Verify sufficient resources (memory, disk)

### Port Conflicts

- TestContainers uses random ports by default
- If issues occur, check for port conflicts

### Slow Tests

- Enable container reuse (already configured)
- Use `@Container` with `reuse = true`
- Consider parallel test execution

## Future Enhancements

- Add more end-to-end scenarios
- Test error recovery scenarios
- Add performance/load tests
- Test concurrent operations
- Add contract tests for API clients

