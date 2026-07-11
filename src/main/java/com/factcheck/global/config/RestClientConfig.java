package com.factcheck.global.config;

import java.net.http.HttpClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    @Bean(name = "aiRestClient")
    public RestClient aiRestClient() {
        // AI 엔진(FastAPI/uvicorn)은 h2c 업그레이드 미지원 — JDK HttpClient 기본값(HTTP_2)이
        // 평문 http:// 요청에 "Upgrade: h2c"를 붙이면 uvicorn(h11)이 바디를 파싱하지 못해
        // 422(body Field required)가 난다. HTTP/1.1로 고정해 업그레이드 시도를 차단한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .baseUrl(aiServerUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
