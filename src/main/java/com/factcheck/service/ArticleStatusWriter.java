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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Long articleId, ArticleStatus status) {
        articleRepository.updateStatus(articleId, status);
        log.debug("기사 상태 변경: articleId={}, status={}", articleId, status);
    }
}
