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
