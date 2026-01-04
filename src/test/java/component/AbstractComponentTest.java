package com.bugbounty.component;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Base class for component tests using Spring Boot Test with TestContainers.
 * Provides shared container setup for PostgreSQL and Redis.
 * All component tests extend this class to get containerized test environment.
 * 
 * Containers are static and shared across all test classes extending this base class.
 * They are started once before any tests run and remain alive for the entire test suite.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        // Ensure context caching works properly - larger cache to reduce context reloads
        "spring.test.context.cache.maxSize=64",
        // Disable scheduling in tests
        "spring.task.scheduling.enabled=false",
        // HikariCP settings for tests - fail fast
        "spring.datasource.hikari.connection-timeout=5000",
        "spring.datasource.hikari.maximum-pool-size=5",
        "spring.datasource.hikari.minimum-idle=1",
        "spring.datasource.hikari.max-lifetime=30000",
        "spring.datasource.hikari.idle-timeout=10000",
        "spring.datasource.hikari.leak-detection-threshold=5000"
    }
)
@Testcontainers
@ActiveProfiles("component-test")
@Import(ComponentTestConfiguration.class)
public abstract class AbstractComponentTest {

    /**
     * Shared PostgreSQL container for all component tests.
     * Static container ensures it's started once and shared across all test classes.
     * Container stays alive for the entire test suite execution.
     */
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("bugbounty_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(false)  // Disable reuse to ensure clean state for each test run
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(30));

    /**
     * Shared Redis container for all component tests.
     * Static container ensures it's started once and shared across all test classes.
     * Container stays alive for the entire test suite execution.
     */
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(false)  // Disable reuse to ensure clean state for each test run
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(30));

    /**
     * Configure Spring properties with TestContainer dynamic values.
     * This method is called by Spring before context initialization.
     * TestContainers automatically starts containers before this method is called.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Override Spring properties with TestContainer values
        // Using method references ensures values are resolved when needed
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
    }
}

