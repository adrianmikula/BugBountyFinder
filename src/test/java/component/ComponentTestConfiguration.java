package com.bugbounty.component;

import org.springframework.ai.chat.ChatClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for component tests using TestContainers.
 * Containers are managed by AbstractComponentTest via @Container annotations.
 * 
 * This configuration:
 * - Provides a default mock ChatClient to prevent Spring AI from trying to connect to Ollama
 * - Tests that need LLM functionality can override with @MockBean
 * - Scheduled tasks are disabled via properties in application-component-test.yml
 */
@TestConfiguration
@Profile("component-test")
public class ComponentTestConfiguration {
    
    /**
     * Default mock ChatClient to prevent Spring AI auto-configuration from failing.
     * Tests that need specific LLM behavior should use @MockBean to override this.
     */
    @Bean
    @Primary
    public ChatClient chatClient() {
        return mock(ChatClient.class);
    }
}

