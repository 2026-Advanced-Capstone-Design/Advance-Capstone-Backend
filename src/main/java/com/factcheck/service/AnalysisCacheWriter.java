package com.factcheck.service;

import com.factcheck.domain.AnalysisCache;
import com.factcheck.domain.Article;
import com.factcheck.repository.AnalysisCacheRepository;
import com.factcheck.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * URL 분석 결과를 캐시에 기록/갱신하는 전담 빈.
 *
 * <p>캐시는 <b>최적화</b>일 뿐이므로, 캐시 쓰기가 실패해도 콜백 본류(결과 저장·DONE 전이)를
 * 망가뜨리면 안 된다. 그래서 {@code REQUIRES_NEW}로 <b>독립 트랜잭션</b>에서 수행하고,
 * 동시 요청 경합으로 인한 UNIQUE 위반은 상위에서 best-effort로 무시한다.
 *
 * <p>또한 캐시는 <b>DONE된 기사에 대해서만</b> 저장된다(호출 시점이 콜백의 DONE 처리 직후).
 * → 미완료/실패 분석이 캐시로 서빙되던 결함(B)을 원천 차단.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCacheWriter {

    private final AnalysisCacheRepository analysisCacheRepository;
    private final ArticleRepository articleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cacheDoneResult(String urlHash, Long articleId) {
        Article ref = articleRepository.getReferenceById(articleId);
        analysisCacheRepository.findByUrlHash(urlHash).ifPresentOrElse(
                existing -> existing.refresh(ref),   // 재분석: 기존 행을 새 기사로 갱신 + TTL 리셋
                () -> analysisCacheRepository.save(AnalysisCache.builder()
                        .urlHash(urlHash)
                        .article(ref)
                        .build())
        );
    }
}
