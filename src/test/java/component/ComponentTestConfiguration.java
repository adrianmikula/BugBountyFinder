package com.bugbounty.component;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for component tests using TestContainers.
 * Containers are managed by AbstractComponentTest via @Container annotations.
 * This configuration class is kept for any additional test-specific beans if needed.
 */
@TestConfiguration
@Profile("component-test")
public class ComponentTestConfiguration {
    // Containers are managed by AbstractComponentTest via @Container annotations
    // No need to create bean definitions here as TestContainers JUnit extension handles them
}

