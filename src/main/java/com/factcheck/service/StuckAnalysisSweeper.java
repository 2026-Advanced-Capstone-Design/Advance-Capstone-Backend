package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.Article;
import com.factcheck.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * stuck된 분석 기사를 주기적으로 재조정하는 스위퍼
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StuckAnalysisSweeper {

    private static final List<ArticleStatus> IN_PROGRESS =
            List.of(ArticleStatus.PENDING, ArticleStatus.ANALYZING);

    private final ArticleRepository articleRepository;
    private final ArticleStatusWriter statusWriter;

    // 10분 넘게 미완료인 경우 stuck
    @Value("${analysis.sweeper.stuck-minutes:10}")
    private long stuckMinutes;

    // 한번에 최대 100건
    @Value("${analysis.sweeper.batch-size:100}")
    private int batchSize;

    // 1분 마다 자동으로 호출이 되어라
    @Scheduled(fixedDelayString = "${analysis.sweeper.interval-ms:60000}")
    public void sweep() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckMinutes);
        List<Article> stuck = articleRepository.findStuck(IN_PROGRESS, threshold, PageRequest.of(0, batchSize));
        if (stuck.isEmpty()) {
            return;
        }

        int reconciled = 0;
        for (Article article : stuck) {
            // 조건부 전이: 조회 이후 정상 콜백이 도착해 상태가 바뀌었다면 덮지 않고 스킵.
            boolean transitioned = statusWriter.updateStatus(
                    article.getId(), article.getStatus(), ArticleStatus.FAILED);
            if (transitioned) {
                reconciled++;
                log.warn("stuck 분석 재조정: articleId={}, {} → FAILED (생성 후 {}분 초과 미완료)",
                        article.getId(), article.getStatus(), stuckMinutes);
            }
        }
        log.info("stuck 분석 스윕 완료: 후보 {}건 중 {}건 FAILED 전이", stuck.size(), reconciled);
    }
}
