package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.Article;
import com.factcheck.dto.request.AiAnalyzeRequest;
import com.factcheck.dto.response.AiAnalyzeResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class AiWorkerClient {

    private final RestClient aiRestClient; // Flask에 HTTP 요청 보내는 클라이언트
    private final ArticleStatusWriter statusWriter; // 기사 상태를 짧은 트랜잭션으로 UPDATE
    private final MeterRegistry meterRegistry; //프로메테우스용 지표수집

    public AiWorkerClient(@Qualifier("aiRestClient") RestClient aiRestClient,
                          ArticleStatusWriter statusWriter,
                          MeterRegistry meterRegistry) {
        this.aiRestClient = aiRestClient;
        this.statusWriter = statusWriter;
        this.meterRegistry = meterRegistry;
    }

    /**
     * aiWorkerExecutor 스레드풀에서 실행하라는 선언
     * AI 분석 요청을 동시에 최대 8개까지 병렬 처리
     */
    @Async("aiWorkerExecutor")
    public void submitAnalysis(Article article) {

        statusWriter.updateStatus(article.getId(), ArticleStatus.PENDING, ArticleStatus.ANALYZING);

        AiAnalyzeRequest request = AiAnalyzeRequest.builder()
                .articleId(article.getId())
                .inputType(article.getInputType().name())
                .text(article.getOriginalText())
                .sourceUrl(article.getSourceUrl())
                .build();

        // AI 호출 지연시간 측정
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            // 파일 호출이 아닌 HTTP 요청을 호촐 HTTP 로 통신 하는 내장 메서드
            // RestClient Config 참조
            AiAnalyzeResponse response = aiRestClient.post()
                    .uri("/analyze")
                    .body(request)
                    .retrieve()
                    .body(AiAnalyzeResponse.class);

            log.info("AI " + "" + "분석 요청 완료: articleId={}, taskId={}",
                    article.getId(), response != null ? response.getTaskId() : "null");

        } catch (RestClientException e) {
            outcome = "failure";
            log.error("AI 서버 호출 실패: articleId={}, error={}", article.getId(), e.getMessage());
            // 조건부 전이 :  이미 DONE/다른 상태면 덮지 않는다.
            statusWriter.updateStatus(article.getId(), ArticleStatus.ANALYZING, ArticleStatus.FAILED);
        } finally {
            sample.stop(Timer.builder("ai.analyze.request")
                    .description("AI 엔진 /analyze 호출 지연시간")
                    .tag("outcome", outcome)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}
