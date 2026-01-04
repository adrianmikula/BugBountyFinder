# Test Setup Fixes

## Issues Identified and Resolved

### 1. Duplicate Spring Configuration Keys
**Problem**: `application-component-test.yml` had duplicate `spring:` keys, causing YAML parsing conflicts.

**Fix**: Merged all Spring configuration under a single `spring:` key.

### 2. Liquibase Conflicts with Hibernate
**Problem**: Liquibase was enabled in component tests while using Hibernate's `ddl-auto: create-drop`, causing schema conflicts.

**Fix**: Disabled Liquibase in component tests (`spring.liquibase.enabled: false`) since we use Hibernate for schema creation in tests.

### 3. Container Reuse Issues
**Problem**: TestContainers with `withReuse(true)` can cause state issues between test runs.

**Fix**: Changed to `withReuse(false)` to ensure clean state for each test run.

### 4. Scheduled Tasks Interference
**Problem**: Scheduled tasks (`@Scheduled`) were running during tests, causing interference and potential context loading issues.

**Fix**: Disabled scheduling in component tests via `spring.task.scheduling.enabled: false`.

### 5. Context Caching Configuration
**Problem**: Spring's context caching wasn't properly configured, leading to repeated context load attempts.

**Fix**: Added explicit context cache configuration in `@SpringBootTest` annotation.

## Configuration Changes

### `application-component-test.yml`
- Fixed duplicate `spring:` keys
- Disabled Liquibase (`spring.liquibase.enabled: false`)
- Disabled scheduled tasks (`spring.task.scheduling.enabled: false`)
- Disabled Spring AI Ollama (`spring.ai.ollama.enabled: false`) to prevent connection attempts

### `AbstractComponentTest.java`
- Disabled container reuse (`withReuse(false)`)
- Added context caching properties
- Added scheduling disable property

### `ComponentTestConfiguration.java`
- Provides default mock `ChatClient` bean to prevent Spring AI from trying to connect to Ollama
- Tests that need specific LLM behavior can override with `@MockBean`
- Scheduling disabled via properties

## Best Practices for Component Tests

1. **Always disable Liquibase** when using Hibernate `ddl-auto` in tests
2. **Disable scheduled tasks** to prevent interference
3. **Don't reuse containers** unless absolutely necessary (and then ensure proper cleanup)
4. **Use single Spring configuration key** in YAML files
5. **Configure context caching** explicitly for better test performance
6. **Provide mock ChatClient** to prevent Spring AI from trying to connect to Ollama
7. **Disable Spring AI auto-configuration** in test properties if not needed

## Running Component Tests

```bash
# Run all component tests
mise run test-component

# Run specific component test
./gradlew test --tests "com.bugbounty.component.BountyFilteringComponentTest"
```

## Troubleshooting

If you still see context loading issues:

1. **Clear test context cache**: Delete `build/test-results` and `build/classes/test`
2. **Check for circular dependencies**: Ensure no circular bean dependencies
3. **Verify TestContainers**: Ensure Docker is running and containers can start
4. **Check logs**: Enable debug logging to see context loading details

```yaml
logging:
  level:
    org.springframework.test.context: DEBUG
    org.springframework.context: DEBUG
```

