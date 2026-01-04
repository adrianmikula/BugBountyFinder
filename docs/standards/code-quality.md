# Code Quality Standards

## Code Review Checklist

Before submitting code for review, ensure:

- [ ] All tests pass (`./gradlew test`)
- [ ] Code coverage meets minimum requirements (80%)
- [ ] No linter warnings or errors
- [ ] Code follows SOLID principles
- [ ] No code duplication (DRY)
- [ ] Code is simple and readable (KISS)
- [ ] Proper error handling implemented
- [ ] Documentation updated (JavaDoc, README)
- [ ] No security vulnerabilities
- [ ] Performance considerations addressed

## Code Smells to Avoid

### 1. Long Methods

**Bad:**
```java
public void processBounty(Bounty bounty) {
    // 50 lines of code doing multiple things
    if (bounty.getAmount() != null && bounty.getAmount().compareTo(new BigDecimal("50")) > 0) {
        if (!repository.existsByIssueIdAndPlatform(bounty.getIssueId(), bounty.getPlatform())) {
            // ... more nested logic
        }
    }
}
```

**Good:**
```java
public void processBounty(Bounty bounty) {
    if (!isEligible(bounty)) {
        return;
    }
    
    saveBounty(bounty);
    enqueueForTriage(bounty);
}

private boolean isEligible(Bounty bounty) {
    return meetsMinimumAmount(bounty) && !isDuplicate(bounty);
}
```

### 2. Large Classes

**Rule**: If a class exceeds 300 lines, consider splitting responsibilities.

**Indicators**:
- Multiple reasons to change
- Hard to test
- Many dependencies
- Unclear purpose

**Solution**: Apply Single Responsibility Principle

### 3. Deep Nesting

**Bad:**
```java
if (condition1) {
    if (condition2) {
        if (condition3) {
            // Actual logic buried deep
        }
    }
}
```

**Good:**
```java
if (!condition1 || !condition2 || !condition3) {
    return;
}
// Actual logic at top level
```

### 4. Magic Numbers/Strings

**Bad:**
```java
if (bounty.getAmount().compareTo(new BigDecimal("50.00")) > 0) {
    // What does 50.00 mean?
}
```

**Good:**
```java
private static final BigDecimal MINIMUM_BOUNTY_AMOUNT = new BigDecimal("50.00");

if (bounty.getAmount().compareTo(MINIMUM_BOUNTY_AMOUNT) > 0) {
    // Clear intent
}
```

### 5. Commented-Out Code

**Rule**: Delete commented-out code. Use version control for history.

**Bad:**
```java
// if (oldCondition) {
//     doSomething();
// }
```

**Good:**
```java
// Removed old condition - see commit abc123 for history
```

### 6. Inconsistent Naming

**Bad:**
```java
void getBounties() { }  // Should be 'fetch' or 'retrieve'
void process() { }      // Too vague
void doStuff() { }      // Meaningless
```

**Good:**
```java
Flux<Bounty> fetchBounties() { }
void processPushEvent(GitHubPushEvent event) { }
void enqueueBountyForTriage(Bounty bounty) { }
```

## Design Patterns

### 1. Builder Pattern

**Use for**: Complex object construction

```java
// ✅ Good: Builder pattern
Bounty bounty = Bounty.builder()
        .issueId("issue-123")
        .platform("algora")
        .amount(new BigDecimal("100.00"))
        .status(BountyStatus.OPEN)
        .build();
```

### 2. Strategy Pattern

**Use for**: Interchangeable algorithms

```java
// ✅ Good: Strategy pattern
public interface ApiClient {
    Flux<Bounty> fetchBounties();
}

@Component
public class AlgoraApiClientImpl implements ApiClient { }
@Component
public class PolarApiClientImpl implements ApiClient { }
```

### 3. Template Method Pattern

**Use for**: Common algorithm structure with variations

```java
// ✅ Good: Template method
public abstract class AbstractComponentTest {
    @BeforeEach
    void setUp() {
        setupContainers();
        configureProperties();
    }
    
    protected abstract void setupContainers();
}
```

### 4. Factory Pattern

**Use for**: Object creation logic

```java
// ✅ Good: Factory
@Component
public class BountyFactory {
    public Bounty createFromApiResponse(JsonNode node) {
        return Bounty.builder()
                .issueId(node.get("id").asText())
                // ... mapping logic
                .build();
    }
}
```

## Error Handling Patterns

### 1. Fail Fast

```java
// ✅ Good: Validate early
public void processBounty(Bounty bounty) {
    if (bounty == null) {
        throw new IllegalArgumentException("Bounty cannot be null");
    }
    // Continue processing
}
```

### 2. Graceful Degradation

```java
// ✅ Good: Fallback behavior
public Flux<Bounty> fetchBounties() {
    return apiClient.fetchBounties()
            .onErrorResume(error -> {
                log.error("Failed to fetch bounties", error);
                return Flux.empty(); // Graceful fallback
            });
}
```

### 3. Specific Exceptions

```java
// ✅ Good: Specific exception types
public class BountyNotFoundException extends RuntimeException {
    public BountyNotFoundException(String issueId) {
        super("Bounty not found: " + issueId);
    }
}

// ❌ Bad: Generic exceptions
throw new RuntimeException("Error");
```

## Logging Standards

### Log Levels

- **ERROR**: System errors, exceptions, failures
- **WARN**: Recoverable issues, deprecated usage
- **INFO**: Important business events, state changes
- **DEBUG**: Detailed debugging information
- **TRACE**: Very detailed tracing (rarely used)

### Logging Best Practices

```java
// ✅ Good: Structured logging with context
log.info("Processing bounty: {} from platform: {}", 
        bounty.getIssueId(), 
        bounty.getPlatform());

log.error("Failed to fetch bounties from API", exception);

// ❌ Bad: String concatenation
log.info("Processing bounty: " + bounty.getIssueId());
```

### Sensitive Data

**Never log**:
- Passwords
- API keys
- Tokens
- Personal information
- Credit card numbers

```java
// ❌ Bad
log.debug("API key: {}", apiKey);

// ✅ Good
log.debug("API call completed successfully");
```

## Performance Guidelines

### 1. Lazy Evaluation

```java
// ✅ Good: Lazy evaluation with reactive streams
return apiClient.fetchBounties()
        .filter(this::isEligible)
        .take(10); // Only process first 10

// ❌ Bad: Eager evaluation
List<Bounty> all = apiClient.fetchBounties().collectList().block();
return all.stream().filter(this::isEligible).limit(10);
```

### 2. Connection Pooling

```java
// ✅ Good: Reuse connections
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create(ConnectionProvider.create("pool", 50))
                ))
                .build();
    }
}
```

### 3. Caching

```java
// ✅ Good: Cache expensive operations
@Cacheable(value = "bounties", key = "#issueId")
public Bounty findBounty(String issueId) {
    return repository.findByIssueId(issueId);
}
```

### 4. Batch Operations

```java
// ✅ Good: Batch database operations
public void saveBounties(List<Bounty> bounties) {
    repository.saveAll(bounties);
}

// ❌ Bad: Individual operations
bounties.forEach(bounty -> repository.save(bounty));
```

## Security Best Practices

### 1. Input Validation

```java
// ✅ Good: Validate all inputs
public void processWebhook(String payload, String signature) {
    if (payload == null || payload.isEmpty()) {
        throw new IllegalArgumentException("Payload cannot be empty");
    }
    
    if (!signatureService.verifySignature(payload, signature)) {
        throw new SecurityException("Invalid signature");
    }
    
    // Process webhook
}
```

### 2. SQL Injection Prevention

```java
// ✅ Good: Use JPA (parameterized queries)
@Query("SELECT b FROM BountyEntity b WHERE b.issueId = :issueId")
Optional<BountyEntity> findByIssueId(@Param("issueId") String issueId);

// ❌ Bad: String concatenation
String query = "SELECT * FROM bounties WHERE issue_id = '" + issueId + "'";
```

### 3. Secret Management

```java
// ✅ Good: Use environment variables
@Value("${app.webhooks.github.secret}")
private String webhookSecret;

// ❌ Bad: Hardcoded secrets
private String webhookSecret = "my-secret-key";
```

## Documentation Standards

### JavaDoc Requirements

**Required for**:
- Public classes
- Public methods
- Complex private methods
- Interfaces

**Example**:
```java
/**
 * Polls Algora API for new bounties and processes them.
 * 
 * <p>This method performs the following steps:
 * <ol>
 *   <li>Fetches bounties from Algora API</li>
 *   <li>Filters duplicates and minimum amount</li>
 *   <li>Saves new bounties to database</li>
 *   <li>Enqueues eligible bounties for triage</li>
 * </ol>
 * 
 * @param minimumAmount Minimum bounty amount to consider (must be positive)
 * @return Flux of discovered bounties
 * @throws ApiException if API call fails
 * @throws IllegalArgumentException if minimumAmount is null or negative
 */
public Flux<Bounty> pollAlgora(BigDecimal minimumAmount) {
    // Implementation
}
```

### README Updates

Update README when:
- Adding new features
- Changing setup procedures
- Modifying configuration
- Adding dependencies

## Code Metrics

### Target Metrics

- **Cyclomatic Complexity**: < 10 per method
- **Class Size**: < 300 lines
- **Method Length**: < 50 lines
- **Parameter Count**: < 5 per method
- **Depth of Inheritance**: < 5 levels

### Tools

- **JaCoCo**: Code coverage
- **SpotBugs**: Static analysis
- **Checkstyle**: Code style
- **PMD**: Code quality

## Refactoring Guidelines

### When to Refactor

- ✅ Tests are passing
- ✅ Code works correctly
- ✅ Opportunity to improve design
- ✅ Reducing technical debt

### Refactoring Safety

1. **Ensure tests pass** before refactoring
2. **Refactor in small steps**
3. **Run tests after each step**
4. **Commit frequently**

### Common Refactorings

- Extract method
- Extract class
- Rename variable/method
- Remove duplication
- Simplify conditional
- Replace magic number with constant

## Continuous Improvement

### Regular Reviews

- Weekly code review sessions
- Monthly architecture reviews
- Quarterly technical debt assessment

### Learning

- Stay updated with Java best practices
- Review industry standards
- Share knowledge with team
- Attend conferences/meetups

