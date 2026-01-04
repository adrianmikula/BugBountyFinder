# Coding Standards

This document outlines the coding standards and best practices for the Bug Bounty Finder project. These standards are based on 2026 industry best practices and ensure maximum code quality, maintainability, and team productivity.

## Core Principles

### 1. Test-Driven Development (TDD)

**Red-Green-Refactor Cycle:**
1. **Red**: Write a failing test first
2. **Green**: Write minimal code to make it pass
3. **Refactor**: Improve code while keeping tests green

**Rules:**
- ✅ Write tests before implementation
- ✅ Tests must fail for the right reason
- ✅ Write minimal code to pass tests
- ✅ Refactor only when tests are green
- ✅ Maintain high test coverage (>80%)

**Test Structure:**
```java
@Test
@DisplayName("Should create a valid bounty with all required fields")
void shouldCreateValidBounty() {
    // Given
    String issueId = "issue-123";
    BigDecimal amount = new BigDecimal("150.00");
    
    // When
    Bounty bounty = Bounty.builder()
            .issueId(issueId)
            .amount(amount)
            .build();
    
    // Then
    assertNotNull(bounty.getId());
    assertEquals(issueId, bounty.getIssueId());
}
```

### 2. DRY (Don't Repeat Yourself)

**Principles:**
- Extract common logic into reusable methods/classes
- Use inheritance and composition appropriately
- Create utility classes for shared functionality
- Avoid copy-paste code

**Examples:**
- ✅ Shared test base classes (`AbstractComponentTest`)
- ✅ Mapper classes for domain-entity conversion
- ✅ Common configuration classes
- ❌ Duplicate validation logic across services

### 3. KISS (Keep It Simple, Stupid)

**Guidelines:**
- Prefer simple solutions over complex ones
- Avoid premature optimization
- Use language features appropriately (Java 21)
- Write code that is easy to understand

**Examples:**
- ✅ Use `record` for immutable data structures
- ✅ Use `var` for local variables when type is obvious
- ✅ Prefer explicit over implicit
- ❌ Over-engineered abstractions

### 4. SOLID Principles

#### Single Responsibility Principle (SRP)
- Each class should have one reason to change
- Services should have focused responsibilities
- Domain models should represent business concepts only

**Example:**
```java
// ✅ Good: Single responsibility
@Service
public class BountyPollingService {
    // Only handles polling logic
}

// ❌ Bad: Multiple responsibilities
@Service
public class BountyService {
    // Polling, filtering, queueing, persistence...
}
```

#### Open/Closed Principle (OCP)
- Open for extension, closed for modification
- Use interfaces and abstractions
- Prefer composition over inheritance

**Example:**
```java
// ✅ Good: Interface allows different implementations
public interface ApiClient {
    Flux<Bounty> fetchBounties();
}

// ✅ Good: Easy to add new implementations
@Component
public class AlgoraApiClientImpl implements ApiClient { }
@Component
public class PolarApiClientImpl implements ApiClient { }
```

#### Liskov Substitution Principle (LSP)
- Subtypes must be substitutable for their base types
- Maintain behavioral contracts
- Don't violate interface expectations

#### Interface Segregation Principle (ISP)
- Clients shouldn't depend on interfaces they don't use
- Create focused, specific interfaces
- Avoid "fat" interfaces

**Example:**
```java
// ✅ Good: Focused interface
public interface GitOperations {
    void cloneRepository(String url, String path);
    void pull(Repository repository);
}

// ❌ Bad: Too many responsibilities
public interface GitOperations {
    void cloneRepository(...);
    void pull(...);
    void createBranch(...);
    void merge(...);
    void rebase(...);
    // ... 20 more methods
}
```

#### Dependency Inversion Principle (DIP)
- Depend on abstractions, not concretions
- High-level modules shouldn't depend on low-level modules
- Both should depend on abstractions

**Example:**
```java
// ✅ Good: Depends on interface
@Service
public class RepositoryService {
    private final GitOperations gitOperations; // Interface
    
    public RepositoryService(GitOperations gitOperations) {
        this.gitOperations = gitOperations;
    }
}

// ❌ Bad: Depends on concrete class
@Service
public class RepositoryService {
    private final JGitOperations gitOperations; // Concrete class
}
```

## Code Organization

### Package Structure
```
com.bugbounty/
├── bounty/
│   ├── domain/          # Domain models
│   ├── entity/          # JPA entities
│   ├── repository/      # Data access
│   ├── service/         # Business logic
│   ├── mapper/          # Domain-entity mapping
│   └── config/          # Configuration
├── cve/
│   └── [same structure]
└── repository/
    └── [same structure]
```

### Domain-Driven Design (DDD)
- **Domain Models**: Pure business logic, no JPA annotations
- **Entities**: JPA persistence layer, separate from domain
- **Mappers**: Convert between domain and entity layers
- **Services**: Orchestrate domain operations

## Naming Conventions

### Classes
- **Services**: `*Service` (e.g., `BountyPollingService`)
- **Repositories**: `*Repository` (e.g., `BountyRepository`)
- **Entities**: `*Entity` (e.g., `BountyEntity`)
- **DTOs**: `*DTO` or descriptive names (e.g., `GitHubPushEvent`)
- **Mappers**: `*Mapper` (e.g., `BountyMapper`)
- **Config**: `*Config` (e.g., `RedisConfig`)

### Methods
- **Actions**: Verb-based (e.g., `fetchBounties()`, `processPushEvent()`)
- **Queries**: Boolean/descriptive (e.g., `isEligible()`, `meetsMinimumAmount()`)
- **Tests**: `should*` or `when*` (e.g., `shouldCreateValidBounty()`)

### Variables
- **Descriptive**: `bountyAmount` not `amt`
- **Boolean**: `isEligible`, `hasError`, `canProcess`
- **Collections**: Plural (e.g., `bounties`, `repositories`)

## Java 21 Best Practices

### Modern Language Features
- ✅ **Records**: For immutable data structures
- ✅ **Pattern Matching**: For type checking and extraction
- ✅ **Sealed Classes**: For restricted inheritance
- ✅ **Text Blocks**: For multi-line strings
- ✅ **Virtual Threads**: For high-concurrency I/O

### Code Style
```java
// ✅ Good: Use var when type is obvious
var bounty = Bounty.builder()
        .issueId("issue-123")
        .build();

// ✅ Good: Pattern matching
if (result instanceof FilterResult fr && fr.shouldProcess()) {
    processBounty(fr);
}

// ✅ Good: Text blocks for prompts
String prompt = """
        Analyze this bug bounty and determine if it should be processed.
        
        Bounty Details:
        - Issue ID: {issueId}
        - Amount: {amount}
        """;
```

## Testing Standards

### Test Organization
- **Unit Tests**: `src/test/java/unit/`
- **Component Tests**: `src/test/java/component/`
- **Test Naming**: `*Test.java` for unit, `*ComponentTest.java` for component

### Test Structure
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BountyPollingService Tests")
class BountyPollingServiceTest {
    
    @Mock
    private AlgoraApiClient apiClient;
    
    @InjectMocks
    private BountyPollingService service;
    
    @Test
    @DisplayName("Should poll and save new bounties")
    void shouldPollAndSaveNewBounties() {
        // Given
        Bounty bounty = createTestBounty();
        when(apiClient.fetchBounties()).thenReturn(Flux.just(bounty));
        
        // When
        Flux<Bounty> result = service.pollAlgora();
        
        // Then
        StepVerifier.create(result)
                .expectNext(bounty)
                .verifyComplete();
    }
}
```

### Test Coverage
- **Minimum**: 80% line coverage
- **Critical Paths**: 100% coverage
- **Domain Models**: 100% coverage
- **Exclusions**: Config classes, entities, DTOs, main class

## Error Handling

### Principles
- ✅ Fail fast with clear error messages
- ✅ Use appropriate exception types
- ✅ Log errors with context
- ✅ Handle errors at appropriate levels

### Patterns
```java
// ✅ Good: Specific exception
if (repository == null) {
    throw new IllegalArgumentException("Repository cannot be null");
}

// ✅ Good: Graceful degradation
try {
    return apiClient.fetchBounties();
} catch (Exception e) {
    log.error("Failed to fetch bounties", e);
    return Flux.empty(); // Fallback
}
```

## Documentation

### Code Comments
- **When**: Explain "why", not "what"
- **JavaDoc**: For public APIs
- **Inline**: For complex business logic

### JavaDoc Example
```java
/**
 * Polls Algora API for new bounties and processes them.
 * 
 * @param minimumAmount Minimum bounty amount to consider
 * @return Flux of discovered bounties
 * @throws ApiException if API call fails
 */
public Flux<Bounty> pollAlgora(BigDecimal minimumAmount) {
    // Implementation
}
```

## Reactive Programming

### Project Reactor Patterns
- ✅ Use `Flux` for multiple items
- ✅ Use `Mono` for single items
- ✅ Chain operations with `flatMap`, `filter`, `map`
- ✅ Handle errors with `onErrorResume`, `doOnError`

### Example
```java
return apiClient.fetchBounties()
        .filter(bounty -> !existsInDatabase(bounty))
        .filter(bounty -> bounty.meetsMinimumAmount(minimum))
        .flatMap(this::saveAndEnqueue)
        .doOnError(error -> log.error("Polling failed", error));
```

## Security

### Best Practices
- ✅ Validate all external inputs
- ✅ Use parameterized queries (JPA handles this)
- ✅ Verify webhook signatures
- ✅ Sanitize user-provided data
- ✅ Use secure defaults

## Performance

### Guidelines
- ✅ Use virtual threads for I/O-bound operations
- ✅ Leverage reactive streams for backpressure
- ✅ Cache expensive operations (Redis)
- ✅ Use connection pooling
- ✅ Profile before optimizing

## Code Review Checklist

- [ ] Tests written and passing
- [ ] Code follows SOLID principles
- [ ] No code duplication (DRY)
- [ ] Simple and readable (KISS)
- [ ] Proper error handling
- [ ] Documentation updated
- [ ] No security vulnerabilities
- [ ] Performance considerations addressed

