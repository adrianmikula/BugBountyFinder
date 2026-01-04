# Common Code Gotchas and Problems

This document catalogs common pitfalls, gotchas, and problems encountered in the Bug Bounty Finder codebase. Use this as a reference to avoid repeating mistakes.

## Table of Contents

1. [Spring Boot Test Configuration](#spring-boot-test-configuration)
2. [YAML Configuration Issues](#yaml-configuration-issues)
3. [Reactive Programming Pitfalls](#reactive-programming-pitfalls)
4. [Database and Liquibase Issues](#database-and-liquibase-issues)
5. [Mocking and Testing Issues](#mocking-and-testing-issues)
6. [Spring AI / LLM Integration](#spring-ai--llm-integration)
7. [TestContainers Issues](#testcontainers-issues)
8. [Java 21 Specific Issues](#java-21-specific-issues)
9. [Configuration Management](#configuration-management)

---

## Spring Boot Test Configuration

### ❌ Duplicate `@SpringBootTest` Annotations (CRITICAL: Causes Spring Bootstrapping Failures)

**Problem**: Having `@SpringBootTest` on both the base class and the test class causes context loading conflicts and Spring bootstrapping failures.

```java
// ❌ BAD: AbstractComponentTest has @SpringBootTest
@SpringBootTest
public abstract class AbstractComponentTest { }

// ❌ BAD: Test class also has @SpringBootTest - CAUSES BOOTSTRAPPING ISSUES
@SpringBootTest
class MyTest extends AbstractComponentTest { }
```

**Solution**: Only annotate the base class, or use `@ContextConfiguration` if you need different contexts.

```java
// ✅ GOOD: Only base class has @SpringBootTest
@SpringBootTest
public abstract class AbstractComponentTest { }

// ✅ GOOD: Test class extends without annotation
class MyTest extends AbstractComponentTest { }
```

**Impact**: Duplicate `@SpringBootTest` annotations can cause:
- Multiple context loading attempts
- "ApplicationContext failure threshold exceeded" errors
- Test suite failures
- Unpredictable test behavior

### ❌ Scheduled Tasks Running in Tests

**Problem**: `@Scheduled` tasks execute during tests, causing interference and potential context loading failures.

```java
// ❌ BAD: Scheduled task runs during tests
@Scheduled(fixedDelay = 5000)
public void pollForBounties() { }
```

**Solution**: Disable scheduling in test configuration.

```yaml
# ✅ GOOD: application-component-test.yml
spring:
  task:
    scheduling:
      enabled: false
```

Or in test class:
```java
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
```

### ❌ Missing Context Caching Configuration

**Problem**: Spring tries to reload failed contexts repeatedly, hitting failure threshold.

**Solution**: Configure context caching explicitly.

```java
@SpringBootTest(
    properties = {
        "spring.test.context.cache.maxSize=32"
    }
)
```

### ❌ Using `@Configuration` Instead of `@TestConfiguration` in Tests

**Problem**: Using `@Configuration` in test classes can cause Spring to load the configuration in production contexts, leading to conflicts and bootstrapping issues.

```java
// ❌ BAD: @Configuration can be picked up by production context
@Configuration
@Profile("test")
public class TestConfig {
    @Bean
    public ChatClient chatClient() {
        return mock(ChatClient.class);
    }
}
```

**Solution**: Always use `@TestConfiguration` for test-specific configurations. This ensures the configuration is only loaded in test contexts.

```java
// ✅ GOOD: @TestConfiguration is only loaded in test contexts
@TestConfiguration
@Profile("component-test")
public class ComponentTestConfiguration {
    @Bean
    @Primary
    public ChatClient chatClient() {
        return mock(ChatClient.class);
    }
}
```

**Impact**: Using `@Configuration` in tests can cause:
- Configuration conflicts between test and production contexts
- Unexpected bean definitions in production
- Spring context loading failures

### ❌ Using `@Qualifier` on Fields with `@RequiredArgsConstructor` (CRITICAL: Causes Spring Bootstrapping Failures)

**Problem**: `@Qualifier` on fields doesn't work with `@RequiredArgsConstructor` because Lombok generates the constructor without the qualifier annotation, causing "expected single matching bean but found 2" errors.

```java
// ❌ BAD: @Qualifier on field doesn't work with @RequiredArgsConstructor
@Component
@RequiredArgsConstructor
public class AlgoraApiClientImpl {
    @Qualifier("algoraWebClient")
    private final WebClient webClient;  // Spring can't determine which bean to inject!
    private final ObjectMapper objectMapper;
}
```

**Solution**: Use an explicit constructor with `@Qualifier` on the constructor parameter.

```java
// ✅ GOOD: Explicit constructor with @Qualifier on parameter
@Component
public class AlgoraApiClientImpl {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AlgoraApiClientImpl(
            @Qualifier("algoraWebClient") WebClient webClient,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }
}
```

**Impact**: Using `@Qualifier` on fields with `@RequiredArgsConstructor` causes:
- "No qualifying bean of type 'X' available: expected single matching bean but found 2" errors
- Spring context loading failures
- Unsatisfied dependency exceptions

---

## YAML Configuration Issues

### ❌ Duplicate Top-Level Keys (CRITICAL: Causes Spring Bootstrapping Failures)

**Problem**: Duplicate keys in YAML cause parsing conflicts, unpredictable behavior, and **Spring context loading failures**. This is a common cause of "ApplicationContext failure threshold exceeded" errors.

```yaml
# ❌ BAD: Duplicate 'spring:' key - CAUSES SPRING BOOTSTRAPPING ISSUES
spring:
  datasource:
    url: jdbc:postgresql://localhost/db

spring:
  ai:
    ollama:
      base-url: http://localhost:11434
```

**Solution**: Merge all properties under a single key. This is critical for Spring Boot to load the context correctly.

```yaml
# ✅ GOOD: Single 'spring:' key with all properties
spring:
  datasource:
    url: jdbc:postgresql://localhost/db
  ai:
    ollama:
      base-url: http://localhost:11434
```

**Impact**: Duplicate YAML keys can cause:
- Spring context loading failures
- "ApplicationContext failure threshold exceeded" errors
- Unpredictable property resolution
- Test suite failures

### ❌ Environment Variable Not Resolved

**Problem**: Environment variables not properly resolved in YAML.

```yaml
# ❌ BAD: Missing default value
spring:
  datasource:
    password: ${DB_PASSWORD}  # Fails if not set
```

**Solution**: Always provide default values.

```yaml
# ✅ GOOD: Default value provided
spring:
  datasource:
    password: ${DB_PASSWORD:postgres}
```

---

## Reactive Programming Pitfalls

### ❌ Blocking in Reactive Chain

**Problem**: Blocking operations in reactive chains defeat the purpose of reactive programming.

```java
// ❌ BAD: Blocking in reactive chain
public Flux<Bounty> getBounties() {
    List<Bounty> list = repository.findAll(); // Blocking!
    return Flux.fromIterable(list);
}
```

**Solution**: Use reactive repositories and keep chains non-blocking.

```java
// ✅ GOOD: Fully reactive
public Flux<Bounty> getBounties() {
    return repository.findAll(); // Reactive repository
}
```

### ❌ Multiple `onNext()` Calls in `Flux.generate()` (CRITICAL: Causes IllegalStateException)

**Problem**: `Flux.generate()` allows only one `sink.next()` call per generator invocation. Multiple calls cause "More than one call to onNext" errors.

```java
// ❌ BAD: Multiple onNext() calls in Flux.generate()
return Flux.generate(
    () -> 0,
    (index, sink) -> {
        List<Item> items = fetchItems(index);
        for (Item item : items) {
            sink.next(item);  // ERROR: Multiple calls to onNext!
        }
        return index + 1;
    }
);
```

**Solution**: Use `flatMap()` to flatten multiple items from each generated element.

```java
// ✅ GOOD: Generate page indices, then flatMap to emit items
return Flux.generate(
    () -> 0,
    (index, sink) -> {
        PageData page = fetchPage(index);
        sink.next(page);  // Emit one page per invocation
        return index + 1;
    }
)
.flatMap(pageData -> {
    // Parse and emit all items from this page
    return Flux.fromIterable(pageData.getItems());
});
```

**Impact**: Multiple `onNext()` calls in `Flux.generate()` cause:
- `IllegalStateException: More than one call to onNext`
- Reactive stream contract violations
- Application crashes during reactive operations

### ❌ Not Handling Errors in Reactive Chains

**Problem**: Errors in reactive chains can cause silent failures or unexpected behavior.

```java
// ❌ BAD: No error handling
public Mono<Bounty> processBounty(Bounty bounty) {
    return apiClient.fetch(bounty.getId())
        .flatMap(this::saveBounty);
    // What if fetch fails?
}
```

**Solution**: Always handle errors explicitly.

```java
// ✅ GOOD: Error handling
public Mono<Bounty> processBounty(Bounty bounty) {
    return apiClient.fetch(bounty.getId())
        .flatMap(this::saveBounty)
        .onErrorResume(error -> {
            log.error("Failed to process bounty {}", bounty.getId(), error);
            return Mono.error(new ProcessingException("Failed to process", error));
        });
}
```

### ❌ Forgetting to Subscribe

**Problem**: Reactive chains don't execute until subscribed.

```java
// ❌ BAD: Chain never executes
public void processBounties() {
    repository.findAll()
        .flatMap(this::processBounty);
    // Nothing happens - no subscription!
}
```

**Solution**: Always subscribe or return the reactive type.

```java
// ✅ GOOD: Subscribe or return
public Mono<Void> processBounties() {
    return repository.findAll()
        .flatMap(this::processBounty)
        .then();
}

// Or if you need fire-and-forget:
public void processBounties() {
    repository.findAll()
        .flatMap(this::processBounty)
        .subscribe(
            result -> log.info("Processed"),
            error -> log.error("Error", error)
        );
}
```

---

## Database and Liquibase Issues

### ❌ Liquibase Enabled in Tests with Hibernate ddl-auto

**Problem**: Running both Liquibase migrations and Hibernate `ddl-auto` causes schema conflicts.

```yaml
# ❌ BAD: Both enabled
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
  liquibase:
    enabled: true  # Conflicts!
```

**Solution**: Disable Liquibase in tests when using Hibernate for schema.

```yaml
# ✅ GOOD: Disable Liquibase in tests
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
  liquibase:
    enabled: false  # Disabled for tests
```

### ❌ Not Using Transactions in Tests

**Problem**: Database changes persist between tests, causing test pollution.

```java
// ❌ BAD: No transaction rollback
@Test
void shouldSaveBounty() {
    service.saveBounty(bounty);
    // Data persists to next test!
}
```

**Solution**: Use `@Transactional` for automatic rollback.

```java
// ✅ GOOD: Transactional test
@SpringBootTest
@Transactional
class BountyServiceTest {
    @Test
    void shouldSaveBounty() {
        service.saveBounty(bounty);
        // Automatically rolled back after test
    }
}
```

### ❌ Wrong Database Configuration in Tests

**Problem**: Tests connect to production database instead of test database.

**Solution**: Always use test profiles and TestContainers.

```java
@SpringBootTest
@ActiveProfiles("component-test")  // ✅ Use test profile
@Testcontainers
class ComponentTest {
    @Container
    static PostgreSQLContainer<?> postgres = ...;
}
```

---

## Mocking and Testing Issues

### ❌ Over-Mocking

**Problem**: Mocking simple value objects or domain models unnecessarily.

```java
// ❌ BAD: Over-mocking
@Mock
private Bounty bounty;  // Simple domain object - don't mock!

when(bounty.getIssueId()).thenReturn("issue-123");
```

**Solution**: Use real instances for simple objects.

```java
// ✅ GOOD: Use real instance
Bounty bounty = Bounty.builder()
    .issueId("issue-123")
    .amount(new BigDecimal("100.00"))
    .build();
```

### ❌ Not Resetting Mocks Between Tests

**Problem**: Mock state persists between tests, causing flaky tests.

```java
// ❌ BAD: Mock state persists
@Mock
private ChatClient chatClient;

@Test
void test1() {
    when(chatClient.call(any())).thenReturn(response1);
}

@Test
void test2() {
    // chatClient still has response1 stubbed!
}
```

**Solution**: Reset mocks in `@BeforeEach`.

```java
// ✅ GOOD: Reset mocks
@BeforeEach
void setUp() {
    reset(chatClient);  // Clear previous stubs
}
```

### ❌ Incorrect MockBean Usage

**Problem**: Using `@Mock` instead of `@MockBean` in Spring Boot tests.

```java
// ❌ BAD: @Mock doesn't replace Spring bean
@SpringBootTest
class MyTest {
    @Mock
    private ChatClient chatClient;  // Not injected into Spring context!
}
```

**Solution**: Use `@MockBean` to replace Spring beans.

```java
// ✅ GOOD: @MockBean replaces Spring bean
@SpringBootTest
class MyTest {
    @MockBean
    private ChatClient chatClient;  // Replaces Spring bean
}
```

---

## Spring AI / LLM Integration

### ❌ Not Handling LLM Response Parsing Errors

**Problem**: LLM responses may not be valid JSON, causing parsing failures.

```java
// ❌ BAD: No error handling
String json = response.getContent();
JsonNode node = objectMapper.readTree(json);  // May throw!
```

**Solution**: Always handle parsing errors gracefully.

```java
// ✅ GOOD: Error handling
try {
    String json = extractJsonFromResponse(response.getContent());
    JsonNode node = objectMapper.readTree(json);
} catch (Exception e) {
    log.error("Failed to parse LLM response: {}", response.getContent(), e);
    return defaultResponse();
}
```

### ❌ Hardcoding Model Names

**Problem**: Model names hardcoded in code make it hard to change.

```java
// ❌ BAD: Hardcoded model
ChatClient client = new OllamaChatClient("deepseek-coder:6.7b");
```

**Solution**: Use configuration properties.

```yaml
# ✅ GOOD: Configuration
spring:
  ai:
    ollama:
      chat:
        options:
          model: ${OLLAMA_MODEL:deepseek-coder:6.7b}
```

### ❌ Not Mocking LLM in Tests

**Problem**: Tests call real LLM, making them slow and unreliable.

**Solution**: Always mock LLM clients in tests.

```java
// ✅ GOOD: Mock LLM
@MockBean
private ChatClient chatClient;

@Test
void shouldProcessBounty() {
    when(chatClient.call(any(Prompt.class)))
        .thenReturn(mockResponse);
    // Test doesn't call real LLM
}
```

---

## TestContainers Issues

### ❌ Container Reuse Causing State Issues

**Problem**: Reusing containers can cause test pollution.

```java
// ❌ BAD: Reuse can cause state issues
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>()
    .withReuse(true);  // State persists between runs
```

**Solution**: Disable reuse for clean state (or ensure proper cleanup).

```java
// ✅ GOOD: No reuse for clean state
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>()
    .withReuse(false);  // Fresh container each time
```

### ❌ Containers Not Starting Before Tests

**Problem**: Tests run before containers are ready.

**Solution**: TestContainers handles this automatically, but ensure containers are `static`.

```java
// ✅ GOOD: Static containers start before tests
@Container
static PostgreSQLContainer<?> postgres = ...;

// ❌ BAD: Non-static containers may not be ready
@Container
PostgreSQLContainer<?> postgres = ...;
```

### ❌ Not Using DynamicPropertySource

**Problem**: Hardcoded database URLs don't work with TestContainers.

```java
// ❌ BAD: Hardcoded URL
@SpringBootTest(properties = "spring.datasource.url=jdbc:postgresql://localhost:5432/test")
```

**Solution**: Use `@DynamicPropertySource` to inject container URLs.

```java
// ✅ GOOD: Dynamic properties
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
}
```

### ❌ Missing HikariCP Configuration in Tests (CRITICAL: Causes Test Hangs)

**Problem**: Default HikariCP settings (30s connection timeout, long maxLifetime) cause tests to hang when containers are slow to start or connections fail.

```yaml
# ❌ BAD: No HikariCP configuration - defaults cause hangs
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/test
```

**Solution**: Configure HikariCP with shorter timeouts for tests to fail fast.

```yaml
# ✅ GOOD: HikariCP configured for tests
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/test
    hikari:
      connection-timeout: 5000  # 5 seconds instead of 30s
      maximum-pool-size: 5  # Smaller pool for tests
      minimum-idle: 1
      max-lifetime: 30000  # 30 seconds
      idle-timeout: 10000  # 10 seconds
      leak-detection-threshold: 5000  # Detect leaks quickly
```

Or in `@SpringBootTest` properties:

```java
@SpringBootTest(properties = {
    "spring.datasource.hikari.connection-timeout=5000",
    "spring.datasource.hikari.maximum-pool-size=5",
    "spring.datasource.hikari.max-lifetime=30000"
})
```

**Impact**: Missing HikariCP configuration causes:
- Tests hanging for 30+ seconds on connection failures
- Multiple connection pools being created (HikariPool-1, HikariPool-2, etc.)
- Connection validation failures
- Test suite timeouts

### ❌ Missing Container Wait Strategies

**Problem**: TestContainers might not wait for containers to be fully ready before tests start, causing connection failures.

```java
// ❌ BAD: No wait strategy
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
```

**Solution**: Always add wait strategies to ensure containers are ready.

```java
// ✅ GOOD: Wait for container to be ready
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .waitingFor(Wait.forListeningPort())
        .withStartupTimeout(Duration.ofSeconds(30));
```

**Impact**: Missing wait strategies cause:
- Connection refused errors
- Tests failing intermittently
- Race conditions between container startup and test execution

---

## Java 21 Specific Issues

### ❌ Using `var` Unnecessarily

**Problem**: Overusing `var` reduces code readability.

```java
// ❌ BAD: Unclear what type is returned
var result = service.process(data);
var items = repository.findAll();
```

**Solution**: Use `var` only when type is obvious from context.

```java
// ✅ GOOD: Type is clear
Bounty bounty = service.processBounty(data);
Flux<Bounty> bounties = repository.findAll();
```

### ❌ Not Using Records for Immutable Data

**Problem**: Creating verbose classes for simple data holders.

```java
// ❌ BAD: Verbose class
public class FilterResult {
    private final boolean shouldProcess;
    private final double confidence;
    // ... getters, equals, hashCode, toString
}
```

**Solution**: Use records for immutable data.

```java
// ✅ GOOD: Concise record
public record FilterResult(
    boolean shouldProcess,
    double confidence,
    int estimatedTimeMinutes,
    String reason
) {}
```

### ❌ Not Using Pattern Matching

**Problem**: Verbose instanceof checks and casts.

```java
// ❌ BAD: Verbose instanceof
if (obj instanceof String) {
    String str = (String) obj;
    // use str
}
```

**Solution**: Use pattern matching (Java 21).

```java
// ✅ GOOD: Pattern matching
if (obj instanceof String str) {
    // use str directly
}
```

---

## Configuration Management

### ❌ Hardcoding Configuration Values

**Problem**: Configuration values hardcoded in code.

```java
// ❌ BAD: Hardcoded
private static final String API_URL = "https://api.example.com";
private static final int TIMEOUT = 5000;
```

**Solution**: Use `@Value` or `@ConfigurationProperties`.

```java
// ✅ GOOD: Configuration properties
@Value("${app.api.url}")
private String apiUrl;

@Value("${app.api.timeout:5000}")
private int timeout;
```

### ❌ Not Validating Configuration

**Problem**: Invalid configuration causes runtime errors.

**Solution**: Use `@Validated` and validation annotations.

```java
// ✅ GOOD: Validated configuration
@ConfigurationProperties(prefix = "app.bounty")
@Validated
public class BountyProperties {
    @Min(1)
    private int batchSize;
    
    @NotBlank
    private String apiUrl;
}
```

### ❌ Mixing Configuration Sources

**Problem**: Configuration scattered across multiple files without clear hierarchy.

**Solution**: Establish clear configuration hierarchy:
1. Environment variables (highest priority)
2. `application-{profile}.yml`
3. `application.yml` (lowest priority)

---

## Test Code Patterns to Avoid

### ❌ Using JUnit Assertions Instead of AssertJ

**Problem**: JUnit assertions are less readable and provide less helpful error messages.

```java
// ❌ BAD: JUnit assertions
assertNotNull(bounty);
assertEquals("issue-123", bounty.getIssueId());
assertTrue(bounty.getAmount().compareTo(new BigDecimal("100")) > 0);
```

**Solution**: Use AssertJ for fluent, readable assertions.

```java
// ✅ GOOD: AssertJ fluent assertions
assertThat(bounty)
    .isNotNull()
    .extracting(Bounty::getIssueId)
    .isEqualTo("issue-123");
assertThat(bounty.getAmount())
    .isGreaterThan(new BigDecimal("100"));
```

### ❌ Wrong Argument Order in JUnit Assertions

**Problem**: JUnit 5 assertion methods have a specific argument order that's different from JUnit 4.

```java
// ❌ BAD: Wrong argument order (JUnit 4 style)
assertNotNull("Message", result);
assertEquals("Expected", "Actual", "Message");
```

**Solution**: JUnit 5 uses `(actual, message)` or `(actual, expected, message)` order.

```java
// ✅ GOOD: Correct argument order (JUnit 5)
assertNotNull(result, "Message");
assertEquals("Actual", "Expected", "Message");
```

**Note**: This is why AssertJ is preferred - it has a more intuitive API.

### ❌ Not Using `@DisplayName` in Tests

**Problem**: Test method names can be unclear, especially with underscores and abbreviations.

```java
// ❌ BAD: Unclear test name
@Test
void testDequeue() { }
```

**Solution**: Always use `@DisplayName` for human-readable descriptions.

```java
// ✅ GOOD: Clear display name
@Test
@DisplayName("Should dequeue highest priority bounty from queue")
void shouldDequeueHighestPriority() { }
```

### ❌ Not Resetting Mocks Between Tests

**Problem**: Mock state persists between tests, causing test pollution and flaky tests.

```java
// ❌ BAD: Mock state persists
@Mock
private ChatClient chatClient;

@Test
void test1() {
    when(chatClient.call(any())).thenReturn(response1);
}

@Test
void test2() {
    // chatClient still has response1 stubbed!
}
```

**Solution**: Always reset mocks in `@BeforeEach`.

```java
// ✅ GOOD: Reset mocks
@BeforeEach
void setUp() {
    reset(chatClient);  // Clear previous stubs
}
```

### ❌ Blocking in Reactive Tests

**Problem**: Using `.block()` in reactive tests defeats the purpose of reactive programming and can cause issues.

```java
// ❌ BAD: Blocking in reactive test
@Test
void shouldProcessBounties() {
    List<Bounty> bounties = service.getBounties()
        .collectList()
        .block();  // Blocks the thread!
    assertEquals(2, bounties.size());
}
```

**Solution**: Use `StepVerifier` for reactive assertions.

```java
// ✅ GOOD: Reactive testing with StepVerifier
@Test
void shouldProcessBounties() {
    StepVerifier.create(service.getBounties())
        .expectNextCount(2)
        .verifyComplete();
}
```

### ❌ Not Testing Error Scenarios in Reactive Code

**Problem**: Reactive chains can fail, but tests don't verify error handling.

```java
// ❌ BAD: No error testing
@Test
void shouldFetchBounties() {
    StepVerifier.create(service.getBounties())
        .expectNextCount(2)
        .verifyComplete();
    // What if API fails?
}
```

**Solution**: Always test error scenarios.

```java
// ✅ GOOD: Test error handling
@Test
void shouldHandleApiErrors() {
    when(apiClient.fetchBounties())
        .thenReturn(Flux.error(new RuntimeException("API Error")));
    
    StepVerifier.create(service.getBounties())
        .expectError(RuntimeException.class)
        .verify();
}
```

### ❌ Over-Mocking Simple Objects

**Problem**: Mocking simple value objects or domain models unnecessarily.

```java
// ❌ BAD: Over-mocking
@Mock
private Bounty bounty;  // Simple domain object - don't mock!

when(bounty.getIssueId()).thenReturn("issue-123");
```

**Solution**: Use real instances for simple objects.

```java
// ✅ GOOD: Use real instance
Bounty bounty = Bounty.builder()
    .issueId("issue-123")
    .amount(new BigDecimal("100.00"))
    .build();
```

### ❌ Not Using Test Fixtures/Builders

**Problem**: Duplicating test data creation across tests.

```java
// ❌ BAD: Duplicated test data
@Test
void test1() {
    Bounty bounty = Bounty.builder()
        .issueId("issue-123")
        .platform("algora")
        .amount(new BigDecimal("100.00"))
        .status(BountyStatus.OPEN)
        .build();
}

@Test
void test2() {
    Bounty bounty = Bounty.builder()
        .issueId("issue-123")
        .platform("algora")
        .amount(new BigDecimal("100.00"))
        .status(BountyStatus.OPEN)
        .build();
}
```

**Solution**: Create reusable test fixtures or builder methods.

```java
// ✅ GOOD: Reusable test fixture
private Bounty createTestBounty(String issueId) {
    return Bounty.builder()
        .issueId(issueId)
        .platform("algora")
        .amount(new BigDecimal("100.00"))
        .status(BountyStatus.OPEN)
        .build();
}
```

### ❌ Missing `@BeforeEach` Setup

**Problem**: Tests don't reset state, causing test pollution.

```java
// ❌ BAD: No setup, state persists
@Mock
private ChatClient chatClient;

@Test
void test1() {
    when(chatClient.call(any())).thenReturn(response1);
}

@Test
void test2() {
    // Previous test's mocks still active!
}
```

**Solution**: Always use `@BeforeEach` for setup.

```java
// ✅ GOOD: Proper setup
@BeforeEach
void setUp() {
    reset(chatClient);
    // Set up common test data
}
```

### ❌ Not Using Given-When-Then Structure

**Problem**: Tests are hard to understand without clear structure.

```java
// ❌ BAD: Unclear test structure
@Test
void testDequeue() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    Bounty result = service.dequeue();
    assertNotNull(result);
}
```

**Solution**: Use Given-When-Then comments for clarity.

```java
// ✅ GOOD: Clear structure
@Test
@DisplayName("Should dequeue highest priority bounty")
void shouldDequeueHighestPriority() {
    // Given
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    when(zSetOperations.popMax(anyString(), anyLong())).thenReturn(tuples);
    
    // When
    Bounty result = service.dequeue();
    
    // Then
    assertThat(result).isNotNull();
    assertThat(result.getIssueId()).isEqualTo("issue-123");
}
```

## General Best Practices

### ✅ Always Use `@DisplayName` in Tests

```java
// ✅ GOOD: Descriptive test name
@Test
@DisplayName("Should filter out duplicate bounties")
void shouldFilterDuplicates() { }
```

### ✅ Use AssertJ for Assertions

```java
// ✅ GOOD: Fluent assertions
assertThat(bounty)
    .isNotNull()
    .extracting(Bounty::getIssueId)
    .isEqualTo("issue-123");

// ❌ BAD: JUnit assertions
assertEquals("issue-123", bounty.getIssueId());
```

### ❌ Wrong Argument Order in JUnit Assertions

**Problem**: JUnit 5 assertion methods have a specific argument order that's different from JUnit 4.

```java
// ❌ BAD: Wrong argument order (JUnit 4 style)
assertNotNull("Message", result);
assertEquals("Expected", "Actual", "Message");
```

**Solution**: JUnit 5 uses `(actual, message)` or `(actual, expected, message)` order.

```java
// ✅ GOOD: Correct argument order (JUnit 5)
assertNotNull(result, "Message");
assertEquals("Actual", "Expected", "Message");
```

**Note**: This is why AssertJ is preferred - it has a more intuitive API.

### ✅ Log Errors with Context

```java
// ✅ GOOD: Contextual error logging
log.error("Failed to process bounty {} from platform {}", 
    bounty.getId(), bounty.getPlatform(), error);

// ❌ BAD: Generic error
log.error("Error", error);
```

### ✅ Use Builder Pattern for Complex Objects

```java
// ✅ GOOD: Builder pattern
Bounty bounty = Bounty.builder()
    .issueId("issue-123")
    .platform("algora")
    .amount(new BigDecimal("100.00"))
    .status(BountyStatus.OPEN)
    .build();
```

---

## Test Code Patterns Summary

### Patterns to Always Avoid

1. **JUnit assertions instead of AssertJ** - Use `assertThat()` from AssertJ
2. **Wrong argument order in assertions** - JUnit 5: `(actual, expected, message)`
3. **Missing `@DisplayName`** - Always add descriptive display names
4. **Not resetting mocks** - Always reset in `@BeforeEach`
5. **Over-mocking simple objects** - Use real instances for domain models
6. **Blocking in component tests** - Use `StepVerifier` for reactive assertions
7. **Missing error scenario tests** - Always test error handling
8. **No Given-When-Then structure** - Use comments to clarify test flow

### When `.block()` is Acceptable

**Acceptable**:
- Simple unit tests with mocked dependencies
- When testing synchronous behavior
- When the reactive chain is fully mocked

**Not Acceptable**:
- Component tests (use `StepVerifier`)
- Integration tests (use `StepVerifier`)
- When testing error scenarios (use `StepVerifier.expectError()`)
- When testing backpressure or timing (use `StepVerifier`)

## Quick Reference Checklist

Before submitting code, check:

- [ ] No duplicate YAML keys
- [ ] Liquibase disabled in tests (if using Hibernate ddl-auto)
- [ ] Scheduled tasks disabled in tests
- [ ] All reactive chains have error handling
- [ ] All reactive chains are subscribed or returned
- [ ] Mocks reset in `@BeforeEach`
- [ ] Using `@MockBean` not `@Mock` in Spring tests
- [ ] TestContainers use `@DynamicPropertySource`
- [ ] Configuration values come from properties, not hardcoded
- [ ] LLM responses have error handling
- [ ] Tests use `@Transactional` for database cleanup
- [ ] Tests use `@DisplayName` for clarity
- [ ] Using AssertJ instead of JUnit assertions (preferred)
- [ ] Using `StepVerifier` for reactive component tests (`.block()` acceptable in simple unit tests)
- [ ] Given-When-Then structure in tests
- [ ] Mocks reset in `@BeforeEach` to prevent test pollution
- [ ] No over-mocking of simple domain objects

---

## Related Documentation

- [Testing Standards](testing.md)
- [Code Quality Standards](code-quality.md)
- [Component Tests Guide](../../testing/COMPONENT_TESTS.md)

