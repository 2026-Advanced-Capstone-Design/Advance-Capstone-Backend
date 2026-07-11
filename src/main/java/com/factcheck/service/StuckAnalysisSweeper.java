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
 * stuck된 분석 기사를 주기적으로 재조정하는 스위퍼(문제 A2).
 *
 * <p>AI 엔진 → Spring 콜백은 fire-and-forget이라, 콜백이 유실되거나 수신 중 Spring이 다운되면
 * 기사가 {@code ANALYZING}(또는 트리거 유실 시 {@code PENDING})에 <b>영구히 갇힌다</b>
 * (사용자 화면은 계속 "분석 중"). 재조정 주체가 없으면 스스로 빠져나올 수 없다.
 *
 * <p>이 스케줄러가 임계시간을 넘긴 미완료 기사를 찾아 {@code FAILED}로 전이시켜, 사용자가
 * 명확한 실패를 보고 재시도할 수 있게 한다(재시도는 9-3 캐시 수정으로 정상 동작).
 * 전이는 조건부 UPDATE(9-2)를 재사용하므로, 재조정 직전에 뒤늦게 도착한 정상 콜백(DONE)을
 * 덮어쓰지 않는다(현재 상태가 기대와 다르면 0행으로 스킵).
 *
 * <p>TODO(9-4c): {@code task_id}를 저장하고 AI {@code /status/{taskId}}를 폴링해, 실제로 DONE인데
 * 콜백만 유실된 경우 결과를 복구(FAILED 대신)하도록 확장. (AI 엔진 협조 필요)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StuckAnalysisSweeper {

    private static final List<ArticleStatus> IN_PROGRESS =
            List.of(ArticleStatus.PENDING, ArticleStatus.ANALYZING);

    private final ArticleRepository articleRepository;
    private final ArticleStatusWriter statusWriter;

    /** 이 시간(분)을 넘도록 미완료면 stuck으로 간주. 분석은 보통 수십 초라 넉넉히 잡는다. */
    @Value("${analysis.sweeper.stuck-minutes:10}")
    private long stuckMinutes;

    /** 한 번의 스윕에서 처리할 최대 건수(대량 스캔 방지). */
    @Value("${analysis.sweeper.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${analysis.sweeper.interval-ms:60000}")
    public void sweep() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckMinutes);
        List<Article> stuck = articleRepository.findStuck(IN_PROGRESS, threshold, PageRequest.of(0, batchSize));
        if (stuck.isEmpty()) {
            return;
        }

        int reconciled = 0;
        for (Article article : stuck) {
            // 조건부 전이: 조회 이후 정상 콜백이 도착해 상태가 바뀌었다면(예: DONE) 덮지 않고 스킵.
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
