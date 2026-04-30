package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.AnalysisResult;
import com.factcheck.domain.Article;
import com.factcheck.domain.SentenceAnalysis;
import com.factcheck.dto.request.AiCallbackRequest;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.repository.AnalysisResultRepository;
import com.factcheck.repository.ArticleRepository;
import com.factcheck.repository.SentenceAnalysisRepository;
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
    private final SentenceAnalysisRepository sentenceAnalysisRepository;
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

        // 섹션별 편향 결과 → JSON 문자열
        String sectionsJson;
        try {
            sectionsJson = req.getSections() != null
                    ? objectMapper.writeValueAsString(req.getSections())
                    : "[]";
        } catch (JsonProcessingException e) {
            sectionsJson = "[]";
        }

        // 핵심 사실 → summary에 함께 보관
        String keyFactsJson;
        try {
            keyFactsJson = req.getKeyFacts() != null
                    ? objectMapper.writeValueAsString(req.getKeyFacts())
                    : "[]";
        } catch (JsonProcessingException e) {
            keyFactsJson = "[]";
        }

        AnalysisResult result = AnalysisResult.builder()
                .article(article)
                .summary(req.getOneLineSummary())
                .title(req.getTopic())
                .biaSentence(keyFactsJson)
                .sections(sectionsJson)
                .biasDirection(req.getBiasDirection())
                .spectrumLabel(req.getSpectrumLabel())
                .emotionNeutrality(req.getEmotionNeutrality() != null ? req.getEmotionNeutrality().floatValue() : null)
                .factRatio(req.getFactRatio() != null ? req.getFactRatio().floatValue() : null)
                .sourceBalance(req.getSourceBalance() != null ? req.getSourceBalance().floatValue() : null)
                .omissionNeutrality(req.getOmissionNeutrality() != null ? req.getOmissionNeutrality().floatValue() : null)
                .biasScore(req.getBiasScore() != null ? req.getBiasScore().floatValue() : null)
                .totalScore(req.getTotalScore())
                .build();

        analysisResultRepository.save(result);

        // highlighted_sentences → sentence_analyses 저장
        List<AiCallbackRequest.HighlightedSentence> highlights = req.getHighlightedSentences();
        if (highlights != null && !highlights.isEmpty()) {
            for (int i = 0; i < highlights.size(); i++) {
                AiCallbackRequest.HighlightedSentence h = highlights.get(i);
                SentenceAnalysis sa = SentenceAnalysis.builder()
                        .sentenceIndex(i)
                        .sentenceText(h.getSentence())
                        .biasScore(h.getScore() != null ? h.getScore().floatValue() : null)
                        .isHighlighted(true)
                        .highlightReason(h.getType())
                        .analysisResult(result)
                        .article(article)
                        .build();
                sentenceAnalysisRepository.save(sa);
            }
        }

        article.updateStatus(ArticleStatus.DONE);

        log.info("AI 분석 완료 저장: articleId={}, highlights={}건, keywords=[{}]",
                req.getArticleId(),
                highlights != null ? highlights.size() : 0,
                keywordsStr);
    }
}
