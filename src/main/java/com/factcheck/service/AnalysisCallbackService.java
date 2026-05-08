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

    /*
        Article을 정의 한 다음에
        1. Article을 조회하고
        2. JSON을 직렬화 한다.
        3.AnalysisResult 저장\
        4. SentenceAnalysis 저장
        5. 완료처리
     */
    @Transactional
    public void handleCallback(AiCallbackRequest req) {
        Article article = articleRepository.findById(req.getArticleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        if ("FAILED".equals(req.getStatus())) {
            article.updateStatus(ArticleStatus.FAILED);
            log.warn("AI 분석 실패: articleId={}, error={}", req.getArticleId(), req.getError());
            return;
        }

        if (analysisResultRepository.findByArticleId(req.getArticleId()).isPresent()) {
            log.warn("중복 콜백 무시: articleId={}", req.getArticleId());
            return;
        }

        String keywordsStr = req.getKeywords() != null
                ? String.join(", ", req.getKeywords())
                : "";

        String sectionsJson  = toJson(req.getSections());
        String keyFactsJson  = toJson(req.getKeyFacts());
        String keywordsJson  = toJson(req.getKeywords());
        String cleanedText   = req.getCompressedText() != null ? req.getCompressedText() : "";

        AnalysisResult result = AnalysisResult.builder()
                .article(article)
                .title(req.getTopic())
                .keyFacts(keyFactsJson)
                .keywords(keywordsJson)
                .sections(sectionsJson)
                .cleanedText(cleanedText)
                .factRatioSource(req.getFactRatioSource())
                .sectionBiasScore(req.getSectionBiasScore() != null ? req.getSectionBiasScore().floatValue() : null)
                .background(req.getBackground())
                .cotVocabReason(req.getCotVocabReason())
                .cotFramingReason(req.getCotFramingReason())
                .cotCitationReason(req.getCotCitationReason())
                .cotOmissionReason(req.getCotOmissionReason())
                .biasDirection(req.getBiasDirection())
                .biasLabel(req.getBiasLabel())
                .biasConfidence(req.getBiasConfidence() != null ? req.getBiasConfidence().floatValue() : null)
                .biasReason(req.getBiasReason())
                .spectrumLabel(req.getSpectrumLabel())
                .emotionNeutrality(req.getEmotionNeutrality() != null ? req.getEmotionNeutrality().floatValue() : null)
                .factRatio(req.getFactRatio() != null ? req.getFactRatio().floatValue() : null)
                .sourceBalance(req.getSourceBalance() != null ? req.getSourceBalance().floatValue() : null)
                .omissionNeutrality(req.getOmissionNeutrality() != null ? req.getOmissionNeutrality().floatValue() : null)
                .biasScore(req.getBiasScore() != null ? req.getBiasScore().floatValue() : null)
                .totalScore(req.getTotalScore())
                .build();

        analysisResultRepository.save(result);

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

    private String toJson(Object value) {
        if (value == null) return "[]";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
