package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기사 상태(status) 변경만 담당하는 짧은 트랜잭션 전담 빈.
 *
 * <p>왜 별도 빈인가: {@link AiWorkerClient#submitAnalysis}는 AI 엔진에 HTTP 요청을 보내는
 * 오래 걸리는 작업이다. 예전에는 이 메서드 전체가 {@code @Transactional}이라
 * <b>HTTP 응답을 기다리는 내내 DB 커넥션을 점유</b>했다(부하 시 HikariCP 고갈).
 * 트랜잭션 경계를 "상태 UPDATE 한 줄"로 축소하려면 커밋이 그 자리에서 끝나야 하는데,
 * 같은 클래스에 트랜잭션 메서드를 만들어 자기호출하면 프록시 우회로 무효가 된다(Phase 7 교훈).
 * 그래서 <b>다른 빈</b>으로 분리해 프록시를 반드시 경유하도록 한다.
 *
 * <p>{@code REQUIRES_NEW}: 호출자에 트랜잭션이 있든 없든 <b>항상 독립된 짧은 트랜잭션</b>으로
 * 실행하고 즉시 커밋해 커넥션을 곧바로 반납한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleStatusWriter {

    private final ArticleRepository articleRepository;

    /**
     * 조건부 상태 전이. 현재 상태가 {@code expected}일 때만 {@code next}로 바꾼다.
     *
     * <p>서로 다른 스레드(요청 스레드의 ANALYZING 쓰기 vs 콜백 스레드의 DONE 쓰기)가
     * 순서가 엇갈려 커밋돼도, 늦게 온 전이가 앞선 전이를 덮어쓰지 못하게 한다(A4 경합 방지).
     * 예: ANALYZING은 PENDING에서만, FAILED는 ANALYZING에서만 → ANALYZING이 DONE을 절대 덮지 못함.
     *
     * @return 전이가 실제로 일어났으면 true, 현재 상태가 {@code expected}와 달라 무시됐으면 false
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean updateStatus(Long articleId, ArticleStatus expected, ArticleStatus next) {
        int updated = articleRepository.updateStatusIfCurrent(articleId, expected, next);
        if (updated == 0) {
            log.warn("상태 전이 스킵(경합 방지): articleId={}, 기대={} → 요청={} — 현재 상태가 기대와 달라 무시",
                    articleId, expected, next);
            return false;
        }
        log.debug("기사 상태 전이: articleId={}, {} → {}", articleId, expected, next);
        return true;
    }
}
