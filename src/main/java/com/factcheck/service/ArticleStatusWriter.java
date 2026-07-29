package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleStatusWriter {

    private final ArticleRepository articleRepository;

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
