package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.Article;
import com.factcheck.dto.request.AiAnalyzeRequest;
import com.factcheck.dto.response.AiAnalyzeResponse;
import com.factcheck.repository.ArticleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
public class AiWorkerClient {

    private final RestClient aiRestClient;
    private final ArticleRepository articleRepository;

    public AiWorkerClient(@Qualifier("aiRestClient") RestClient aiRestClient,
                          ArticleRepository articleRepository) {
        this.aiRestClient = aiRestClient;
        this.articleRepository = articleRepository;
    }

    /**
     * Flask /analyze 엔드포인트에 비동기로 분석 요청 전달
     * aiWorkerExecutor 스레드풀에서 실행하라는 선언
     * AI 분석 요청을 동시에 최대 8개까지 병렬 처리 가능
     */
    @Async("aiWorkerExecutor")
    @Transactional
    public void submitAnalysis(Article article) {
        updateStatus(article, ArticleStatus.ANALYZING);

        AiAnalyzeRequest request = AiAnalyzeRequest.builder()
                .articleId(article.getId())
                .inputType(article.getInputType().name())
                .text(article.getOriginalText())
                .sourceUrl(article.getSourceUrl())
                .build();

        try {
            // 파일 호출이 아닌 HTTP 요청을 호촐 HTTP 로 통신 하는 내장 메서드
            // RestClient Config 참조
            AiAnalyzeResponse response = aiRestClient.post()
                    .uri("/analyze")
                    .body(request)
                    .retrieve()
                    .body(AiAnalyzeResponse.class);

            log.info("AI 분석 요청 완료: articleId={}, taskId={}",
                    article.getId(), response != null ? response.getTaskId() : "null");

        } catch (RestClientException e) {
            log.error("AI 서버 호출 실패: articleId={}, error={}", article.getId(), e.getMessage());
            updateStatus(article, ArticleStatus.FAILED);
        }
    }

    private void updateStatus(Article article, ArticleStatus status) {
        articleRepository.findById(article.getId()).ifPresent(a -> {
            a.updateStatus(status);
            articleRepository.save(a);
        });
    }
}
