# Testing Standards

## Overview

This project follows **Test-Driven Development (TDD)** principles. All code must be written with tests first, ensuring high quality and maintainability.

## Test Types

### 1. Unit Tests

**Purpose**: Test individual components in isolation

**Location**: `src/test/java/unit/`

**Characteristics**:
- Fast execution (no external dependencies)
- Mock external dependencies
- Test single responsibility
- No database or network calls

**Example**:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BountyPollingService Tests")
class BountyPollingServiceTest {
    
    @Mock
    private AlgoraApiClient apiClient;
    
    @Mock
    private BountyRepository repository;
    
    @InjectMocks
    private BountyPollingService service;
    
    @Test
    @DisplayName("Should filter out duplicate bounties")
    void shouldFilterDuplicates() {
        // Given
        Bounty bounty = createTestBounty("issue-123");
        when(repository.existsByIssueIdAndPlatform("issue-123", "algora"))
                .thenReturn(true);
        when(apiClient.fetchBounties()).thenReturn(Flux.just(bounty));
        
        // When
        Flux<Bounty> result = service.pollAlgora();
        
        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}
```

### 2. Component Tests

**Purpose**: Test major features with real infrastructure

**Location**: `src/test/java/component/`

**Characteristics**:
- Use TestContainers for real databases/services
- Test integration between components
- Verify end-to-end workflows
- Slower execution (requires Docker)

**Example**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("component-test")
class BountyPollingComponentTest extends AbstractComponentTest {
    
    @Autowired
    private BountyPollingService pollingService;
    
    @Autowired
    private BountyRepository repository;
    
    @Test
    @DisplayName("Should poll, save, and enqueue bounties")
    void shouldPollSaveAndEnqueue() {
        // Given
        // Mock external API responses
        
        // When
        Flux<Bounty> result = pollingService.pollAllPlatforms();
        
        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
        
        // Verify database
        assertEquals(2, repository.count());
    }
}
```

## Test Structure

### Given-When-Then Pattern

All tests should follow the **Given-When-Then** structure:

```java
@Test
@DisplayName("Should create a valid bounty")
void shouldCreateValidBounty() {
    // Given - Setup test data and mocks
    String issueId = "issue-123";
    BigDecimal amount = new BigDecimal("150.00");
    
    // When - Execute the code under test
    Bounty bounty = Bounty.builder()
            .issueId(issueId)
            .amount(amount)
            .build();
    
    // Then - Verify the results
    assertNotNull(bounty.getId());
    assertEquals(issueId, bounty.getIssueId());
    assertEquals(amount, bounty.getAmount());
}
```

### Test Naming

- **Format**: `should*` or `when*`
- **Descriptive**: Clearly state what is being tested
- **Use `@DisplayName`**: For human-readable test descriptions

**Examples**:
```java
@Test
@DisplayName("Should filter bounties below minimum amount")
void shouldFilterBountiesBelowMinimumAmount() { }

@Test
@DisplayName("Should enqueue bounty with correct priority")
void shouldEnqueueBountyWithCorrectPriority() { }
```

## Testing Reactive Code

### Project Reactor Testing

Use `StepVerifier` for testing `Flux` and `Mono`:

```java
@Test
@DisplayName("Should process bounties reactively")
void shouldProcessBountiesReactively() {
    // Given
    Bounty bounty1 = createBounty("issue-1");
    Bounty bounty2 = createBounty("issue-2");
    when(apiClient.fetchBounties())
            .thenReturn(Flux.just(bounty1, bounty2));
    
    // When
    Flux<Bounty> result = service.pollAlgora();
    
    // Then
    StepVerifier.create(result)
            .expectNext(bounty1)
            .expectNext(bounty2)
            .verifyComplete();
}
```

### Testing Error Scenarios

```java
@Test
@DisplayName("Should handle API errors gracefully")
void shouldHandleApiErrors() {
    // Given
    when(apiClient.fetchBounties())
            .thenReturn(Flux.error(new RuntimeException("API Error")));
    
    // When
    Flux<Bounty> result = service.pollAlgora();
    
    // Then
    StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
}
```

## Mocking Guidelines

### When to Mock

- ✅ External APIs
- ✅ Database repositories (in unit tests)
- ✅ File system operations
- ✅ Network calls
- ✅ Complex dependencies

### When NOT to Mock

- ❌ Domain models (use real instances)
- ❌ Value objects
- ❌ Simple utility classes
- ❌ The class under test

### Mockito Best Practices

```java
// ✅ Good: Use @Mock and @InjectMocks
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private ServiceUnderTest service;
}

// ✅ Good: Verify interactions
verify(dependency, times(1)).methodCall();

// ✅ Good: Stub return values
when(dependency.getValue()).thenReturn("expected");

// ❌ Bad: Over-mocking
when(domainObject.getSimpleProperty()).thenReturn("value");
// Use real domain objects instead
```

## Test Coverage Requirements

### Minimum Coverage

- **Overall**: 80% line coverage
- **Domain Models**: 100% coverage
- **Services**: 90% coverage
- **Controllers**: 80% coverage
- **Mappers**: 100% coverage

### Exclusions

The following are excluded from coverage:
- Configuration classes (`*Config`)
- JPA entities (`*Entity`)
- DTOs (`*DTO`, `*Event`)
- Application main class
- Lombok-generated code

### Coverage Reports

Coverage is automatically generated after tests:
```bash
# View HTML report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html

# View summary
./gradlew jacocoCoverageSummary
```

## Test Data Management

### Test Fixtures

Create reusable test data builders:

```java
class TestDataBuilder {
    static Bounty createBounty(String issueId) {
        return Bounty.builder()
                .issueId(issueId)
                .platform("algora")
                .amount(new BigDecimal("100.00"))
                .status(BountyStatus.OPEN)
                .build();
    }
    
    static Bounty createHighValueBounty() {
        return createBounty("issue-123")
                .toBuilder()
                .amount(new BigDecimal("500.00"))
                .build();
    }
}
```

### Test Isolation

- Each test should be independent
- Use `@BeforeEach` for setup
- Clean up after tests
- Don't rely on test execution order

## Component Test Best Practices

### TestContainers Usage

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("component-test")
class ComponentTest extends AbstractComponentTest {
    
    // Containers are managed by AbstractComponentTest
    // PostgreSQL and Redis are automatically started
    
    @Test
    void shouldTestWithRealDatabase() {
        // Test with real PostgreSQL
    }
}
```

### Test Configuration

- Use `application-component-test.yml` for test-specific config
- Mock external services (APIs, LLM)
- Use in-memory or containerized services

### Performance Considerations

- Component tests are slower - use sparingly
- Test critical paths end-to-end
- Use container reuse when possible
- Run component tests separately from unit tests

## Assertions

### Prefer AssertJ

```java
// ✅ Good: AssertJ fluent assertions
assertThat(bounty)
        .isNotNull()
        .extracting(Bounty::getIssueId)
        .isEqualTo("issue-123");

assertThat(bounties)
        .hasSize(2)
        .extracting(Bounty::getAmount)
        .containsExactly(
                new BigDecimal("100.00"),
                new BigDecimal("200.00")
        );

// ❌ Avoid: JUnit assertions (less readable)
assertEquals("issue-123", bounty.getIssueId());
```

### Custom Assertions

For complex domain objects, create custom assertions:

```java
class BountyAssert {
    static BountyAssert assertThat(Bounty actual) {
        return new BountyAssert(actual);
    }
    
    BountyAssert hasIssueId(String expected) {
        assertThat(actual.getIssueId()).isEqualTo(expected);
        return this;
    }
    
    BountyAssert isEligible() {
        assertThat(actual.isEligibleForProcessing()).isTrue();
        return this;
    }
}
```

## TDD Workflow

### Red-Green-Refactor

1. **Red**: Write a failing test
   ```java
   @Test
   void shouldCalculatePriority() {
       Bounty bounty = createBounty();
       double priority = service.calculatePriority(bounty);
       assertThat(priority).isGreaterThan(0);
   }
   ```

2. **Green**: Write minimal code to pass
   ```java
   public double calculatePriority(Bounty bounty) {
       return 1000.0; // Minimal implementation
   }
   ```

3. **Refactor**: Improve while keeping tests green
   ```java
   public double calculatePriority(Bounty bounty) {
       return BASE_PRIORITY + bounty.getAmount().doubleValue();
   }
   ```

### Test First Benefits

- ✅ Clear requirements before implementation
- ✅ Better design (testable = better design)
- ✅ Documentation through tests
- ✅ Confidence in refactoring
- ✅ Prevents over-engineering

## Common Testing Patterns

### Testing Async Operations

```java
@Test
void shouldProcessAsync() {
    CompletableFuture<Bounty> future = service.processAsync(bounty);
    
    Bounty result = future.get(5, TimeUnit.SECONDS);
    
    assertThat(result).isNotNull();
}
```

### Testing Scheduled Tasks

```java
@Test
void shouldPollOnSchedule() {
    // Use @TestPropertySource to override schedule
    // Or use Awaitility for async verification
    await().atMost(10, SECONDS)
            .until(() -> repository.count() > 0);
}
```

### Testing Error Handling

```java
@Test
void shouldHandleErrorsGracefully() {
    when(apiClient.fetchBounties())
            .thenReturn(Flux.error(new ApiException()));
    
    Flux<Bounty> result = service.pollAlgora();
    
    StepVerifier.create(result)
            .expectError(ApiException.class)
            .verify();
    
    // Verify fallback behavior
    assertThat(queue.size()).isEqualTo(0);
}
```

## Test Maintenance

### Keep Tests Simple

- One assertion per test (when possible)
- Test one behavior at a time
- Use descriptive test names
- Remove obsolete tests

### Test Documentation

- Use `@DisplayName` for clarity
- Add comments for complex test scenarios
- Document test data requirements
- Explain test setup when non-obvious

