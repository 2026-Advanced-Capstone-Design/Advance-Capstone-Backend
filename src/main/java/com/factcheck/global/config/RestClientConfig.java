package com.factcheck.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Bean(name = "aiRestClient")
    public RestClient aiRestClient() {
        return RestClient.builder()
                .baseUrl(aiServerUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
