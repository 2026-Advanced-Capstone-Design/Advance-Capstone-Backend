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
        // 거부정책(Phase 3): 기본 AbortPolicy는 큐 초과 시 예외 → 제출 요청이 500으로 터지고
        // 해당 분석은 영구 유실(PENDING stuck → 스위퍼가 FAILED 처리)된다.
        // CallerRunsPolicy는 초과분을 호출 스레드(Tomcat 요청 스레드)가 직접 실행 —
        // Phase 8 이후 submitAnalysis는 짧은 UPDATE + 즉시 202를 받는 빠른 HTTP뿐이라
        // 요청 스레드가 수십 ms 느려지는 대신 작업 유실이 없고, 자연스러운 백프레셔가 된다.
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
