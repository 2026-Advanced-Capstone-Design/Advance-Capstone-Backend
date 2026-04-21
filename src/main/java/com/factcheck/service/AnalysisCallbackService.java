package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.AnalysisResult;
import com.factcheck.domain.Article;
import com.factcheck.dto.request.AiCallbackRequest;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.repository.AnalysisResultRepository;
import com.factcheck.repository.ArticleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCallbackService {

    private final ArticleRepository articleRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void handleCallback(AiCallbackRequest req) {
        Article article = articleRepository.findById(req.getArticleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        if ("FAILED".equals(req.getStatus())) {
            article.updateStatus(ArticleStatus.FAILED);
            log.warn("AI 분석 실패: articleId={}, error={}", req.getArticleId(), req.getError());
            return;
        }

        // 키워드 리스트 → 쉼표 구분 문자열
        String keywordsStr = req.getKeywords() != null
                ? String.join(", ", req.getKeywords())
                : "";

        // 핵심 사실 리스트 → JSON 배열 문자열
        String keyFactsStr;
        try {
            keyFactsStr = req.getKeyFacts() != null
                    ? objectMapper.writeValueAsString(req.getKeyFacts())
                    : "[]";
        } catch (JsonProcessingException e) {
            keyFactsStr = "[]";
        }

        AnalysisResult result = AnalysisResult.builder()
                .article(article)
                .summary(req.getOneLineSummary())
                .title(req.getTopic())
                .biaSentence(keyFactsStr)
                .biasDirection(req.getBiasDirection())
                .spectrumLabel(req.getSpectrumLabel())
                .emotionNeutrality(req.getEmotionNeutrality() != null ? req.getEmotionNeutrality().floatValue() : null)
                .factRatio(req.getFactRatio() != null ? req.getFactRatio().floatValue() : null)
                .sourceBalance(req.getSourceBalance() != null ? req.getSourceBalance().floatValue() : null)
                .biasScore(req.getBiasScore() != null ? req.getBiasScore().floatValue() : null)
                .totalScore(req.getTotalScore())
                .build();

        analysisResultRepository.save(result);
        article.updateStatus(ArticleStatus.DONE);

        log.info("AI 분석 완료 저장: articleId={}, keywords=[{}]",
                req.getArticleId(), keywordsStr);
    }
}
