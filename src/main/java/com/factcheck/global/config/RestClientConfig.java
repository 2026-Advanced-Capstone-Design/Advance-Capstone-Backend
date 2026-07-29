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
        // JDK HttpClient의 기본값은 HTTP/2 이나....
        // 평문 http 요청시.... HTTP/2를 쓰려고 요청에 Upgrade: h2c 헤더를 붙임
        // 근데 AI 서버 FastAPI는 h11 기반이라 h2c 업그레이드를 지원하지 않음
        // 이로인해 에러가 발생 따라서 HTTP 1/1로 고정
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
