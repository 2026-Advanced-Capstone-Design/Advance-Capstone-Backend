package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.AnalysisResult;
import com.factcheck.domain.Article;
import com.factcheck.repository.AnalysisResultRepository;
import com.factcheck.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 조건부 상태 전이(CAS)와 멱등성 검증.
 *
 */
@SpringBootTest
// test 프로파일 활성화
@ActiveProfiles("test")
@DisplayName("조건부 상태 전이 + 멱등성")
class ArticleStatusTransitionTest {

    @Autowired
    private ArticleStatusWriter articleStatusWriter;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private AnalysisResultRepository analysisResultRepository;

    @BeforeEach
    void cleanUp() {
        analysisResultRepository.deleteAll();
        articleRepository.deleteAll();
    }

    @Test
    @DisplayName("기대한 상태이면 전이된다 ")
    void transitionMatching() {
        Long articleId = pendingArticleId();

        boolean applied = articleStatusWriter.updateStatus(
                articleId, ArticleStatus.PENDING, ArticleStatus.ANALYZING);

        assertThat(applied).isTrue();
        assertThat(statusOf(articleId)).isEqualTo(ArticleStatus.ANALYZING);
    }

    @Test
    @DisplayName("DONE이 먼저 커밋되면 늦게 도착한 ANALYZING 전이는 DONE을 덮어쓰지 못한다")
    void lateAnalyzing() {
        Long articleId = pendingArticleId();

        // AI가 빨리 끝나 완료 콜백(DONE)이 먼저 커밋된 상황
        articleStatusWriter.updateStatus(articleId, ArticleStatus.PENDING, ArticleStatus.DONE);

        // 뒤늦게 도착한 PENDING → ANALYZING 전이 (AiWorkerClient.submitAnalysis)
        boolean applied = articleStatusWriter.updateStatus(
                articleId, ArticleStatus.PENDING, ArticleStatus.ANALYZING);

        assertThat(applied).isFalse();
        assertThat(statusOf(articleId)).isEqualTo(ArticleStatus.DONE);
    }

    @Test
    @DisplayName("DONE이 먼저 커밋되면 늦게 도착한 FAILED 전이도 DONE을 덮어쓰지 못한다")
    void lateFailed() {
        Long articleId = pendingArticleId();
        articleStatusWriter.updateStatus(articleId, ArticleStatus.PENDING, ArticleStatus.DONE);

        // AI 호출이 실패로 판정돼 뒤늦게 도착한 ANALYZING → FAILED 전이
        boolean applied = articleStatusWriter.updateStatus(
                articleId, ArticleStatus.ANALYZING, ArticleStatus.FAILED);

        assertThat(applied).isFalse();
        assertThat(statusOf(articleId)).isEqualTo(ArticleStatus.DONE);
    }

    @Test
    @DisplayName("한 기사에 분석 결과를 두 번 저장하면 ARTICLE_ID UNIQUE가 막는다")
    void duplicateResultRejected() {
        Article article = articleRepository.save(Article.createFromText("idempotency test body"));
        analysisResultRepository.saveAndFlush(resultOf(article, "first"));

        // 중복 콜백으로 결과가 한 번 더 저장되려 해도 DB가 최종 방어선이 된다.
        assertThatThrownBy(() -> analysisResultRepository.saveAndFlush(resultOf(article, "second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long pendingArticleId() {
        // Article 생성자가 상태를 PENDING으로 초기화한다.
        return articleRepository.save(Article.createFromText("status transition test body")).getId();
    }

    private AnalysisResult resultOf(Article article, String title) {
        return AnalysisResult.builder()
                .article(article)
                .title(title)
                .keyFacts("[]")
                .keywords("[]")
                .build();
    }

    private ArticleStatus statusOf(Long articleId) {
        return articleRepository.findById(articleId).orElseThrow().getStatus();
    }
}
