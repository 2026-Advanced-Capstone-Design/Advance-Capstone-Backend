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
 * 캐시는 최적화일 뿐이므로, 캐시 쓰기가 실패해도 콜백 본류(결과 저장·DONE 전이)를
 * \망가뜨리면 안 된다. 그래서 독립 트랜잭션 에서 수행하v고,
 * 동시 요청 경합으로 인한 UNIQUE 위반은 상위에서 best-effort로 무시한다.
 *
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
