package com.bugbounty.bounty.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.bounty.platforms.algora.api-url}")
    private String algoraApiUrl;

    @Value("${app.bounty.platforms.polar.api-url}")
    private String polarApiUrl;

    @Value("${app.bounty.platforms.gitpay.api-url:https://api.gitpay.me}")
    private String gitpayApiUrl;

    @Value("${app.bounty.platforms.algora.api-key:}")
    private String algoraApiKey;

    @Value("${app.bounty.platforms.polar.api-key:}")
    private String polarApiKey;

    @Value("${app.bounty.platforms.gitpay.api-key:}")
    private String gitpayApiKey;

    @Value("${app.bounty.platforms.github.api-token:}")
    private String githubApiToken;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean("algoraWebClient")
    public WebClient algoraWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(algoraApiUrl);
        
        // Add API key authentication if provided
        if (algoraApiKey != null && !algoraApiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + algoraApiKey)
                   .defaultHeader("X-API-Key", algoraApiKey);
        }
        
        return builder.build();
    }

    @Bean("polarWebClient")
    public WebClient polarWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(polarApiUrl);
        
        // Add API key authentication if provided
        if (polarApiKey != null && !polarApiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + polarApiKey)
                   .defaultHeader("X-API-Key", polarApiKey);
        }
        
        return builder.build();
    }

    @Bean("gitpayWebClient")
    public WebClient gitpayWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(gitpayApiUrl);
        
        // Add API key authentication if provided
        if (gitpayApiKey != null && !gitpayApiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + gitpayApiKey)
                   .defaultHeader("X-API-Key", gitpayApiKey);
        }
        
        return builder.build();
    }

    @Bean("githubWebClient")
    public WebClient githubWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.github.com");
        
        // Add GitHub PAT authentication if provided
        if (githubApiToken != null && !githubApiToken.isEmpty()) {
            builder.defaultHeader("Authorization", "token " + githubApiToken);
        }
        
        // Add required headers for GitHub API
        builder.defaultHeader("Accept", "application/vnd.github.v3+json");
        
        return builder.build();
    }
    
    @Bean("nvdWebClientBuilder")
    public WebClient.Builder nvdWebClientBuilder() {
        return WebClient.builder();
    }
}

