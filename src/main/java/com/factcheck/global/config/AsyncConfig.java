package com.factcheck.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync

public class AsyncConfig {
/*
     스레드풀: core 4개, max 8개, 대기큐 100개
     반환 타입을 ThreadPoolTaskExecutor
     executor.queued / executor.active / executor.pool.size 등 큐 깊이 지표
 */
    @Bean(name = "aiWorkerExecutor")
    public ThreadPoolTaskExecutor aiWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-worker-");
        // 왜 CallerRunsPolicy 지? 기본값은 큐가 다 차면 누락 시키기 때문에!
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
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
        // OCR도 동일 — 큐 초과 시 요청 스레드에서 실행(느려질 뿐 유실 없음).
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
