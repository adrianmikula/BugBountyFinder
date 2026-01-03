package com.bugbounty.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for component tests using TestContainers.
 * This provides container-based testing similar to Arquillian but for Spring Boot.
 */
@TestConfiguration
@Profile("component-test")
public class ComponentTestConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @Primary
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("bugbounty_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @Primary
    public RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:7-alpine"))
                .withReuse(true);
    }
}

