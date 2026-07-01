package com.factcheck.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync

public class AsyncConfig {
/*
     스레드풀: core 4개, max 8개, 대기큐 100개
     최대 8개 까지의 동시 요청을 처리 하는것이 가능하다.

     반환 타입을 ThreadPoolTaskExecutor로 명시 → Spring Boot가 타입 기반으로
     executor.queued / executor.active / executor.pool.size 등 큐 깊이 지표를 자동 등록.
 */
    @Bean(name = "aiWorkerExecutor")
    public ThreadPoolTaskExecutor aiWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-worker-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "ocrExecutor")
    public ThreadPoolTaskExecutor ocrExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ocr-");
        executor.initialize();
        return executor;
    }
}
