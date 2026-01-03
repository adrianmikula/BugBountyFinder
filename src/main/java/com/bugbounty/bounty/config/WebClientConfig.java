package com.bugbounty.bounty.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${app.bounty.platforms.algora.api-url}")
    private String algoraApiUrl;

    @Value("${app.bounty.platforms.polar.api-url}")
    private String polarApiUrl;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean("algoraWebClient")
    public WebClient algoraWebClient() {
        return WebClient.builder()
                .baseUrl(algoraApiUrl)
                .build();
    }

    @Bean("polarWebClient")
    public WebClient polarWebClient() {
        return WebClient.builder()
                .baseUrl(polarApiUrl)
                .build();
    }
}

