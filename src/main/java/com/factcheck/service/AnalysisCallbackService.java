package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.*;
import com.factcheck.dto.request.AiCallbackRequest;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.repository.*;
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
    private final AnalysisSectionRepository analysisSectionRepository;
    private final SentenceAnalysisRepository sentenceAnalysisRepository;
    private final FactCheckResultRepository factCheckResultRepository;
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

        AnalysisResult result = AnalysisResult.builder()
                .article(article)
                .title(req.getTopic())
                .compressedText(req.getCompressedText())
                .keywords(toJson(req.getKeywords()))
                .factRatioSource(toFloat(req.getFactRatioSource()))
                .biasLabel(req.getBiasLabel())
                .biasConfidence(toFloat(req.getBiasConfidence()))
                .biasReason(req.getBiasReason())
                .biasDirection(req.getBiasDirection())
                .emotionNeutrality(toFloat(req.getEmotionNeutrality()))
                .factRatio(toFloat(req.getFactRatio()))
                .biasScore(toFloat(req.getBiasScore()))
                .totalScore(req.getTotalScore())
                .cotEmotionReason(req.getCotEmotionReason())
                .cotFactRatioReason(req.getCotFactRatioReason())
                .build();

        analysisResultRepository.save(result);

        List<AiCallbackRequest.SectionResult> sections = req.getSections();
        if (sections != null) {
            for (AiCallbackRequest.SectionResult sec : sections) {
                AnalysisSection section = AnalysisSection.builder()
                        .analysisResult(result)
                        .topic(sec.getTopic())
                        .biasLabel(sec.getBiasLabel())
                        .confidence(toFloat(sec.getConfidence()))
                        .reason(sec.getReason())
                        .step1BiasedExpressions(toJson(sec.getStep1BiasedExpressions()))
                        .step2NeutralExpressions(toJson(sec.getStep2NeutralExpressions()))
                        .step3Judgment(sec.getStep3Judgment())
                        .build();
                analysisSectionRepository.save(section);
            }
        }

        List<AiCallbackRequest.HighlightedSentence> highlighted = req.getHighlightedSentences();
        if (highlighted != null) {
            for (AiCallbackRequest.HighlightedSentence hs : highlighted) {
                SentenceAnalysis sentence = SentenceAnalysis.builder()
                        .analysisResult(result)
                        .article(article)
                        .sentenceText(hs.getSentence())
                        .highlightType(hs.getType())
                        .highlightReason(hs.getReason())
                        .highlightScore(toFloat(hs.getScore()))
                        .build();
                sentenceAnalysisRepository.save(sentence);
            }
        }

        List<AiCallbackRequest.FactCheckItem> factChecks = req.getFactCheckResults();
        if (factChecks != null) {
            for (AiCallbackRequest.FactCheckItem fc : factChecks) {
                FactCheckResult factCheck = FactCheckResult.builder()
                        .analysisResult(result)
                        .fact(fc.getFact())
                        .found(fc.getFound())
                        .rating(fc.getRating())
                        .score(fc.getScore() != null ? fc.getScore().floatValue() : null)
                        .title(fc.getTitle())
                        .publisher(fc.getPublisher())
                        .url(fc.getUrl())
                        .build();
                factCheckResultRepository.save(factCheck);
            }
        }

        article.updateStatus(ArticleStatus.DONE);
        log.info("AI 분석 완료 저장: articleId={}", req.getArticleId());
    }

    private String toJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private Float toFloat(Double d) {
        return d != null ? d.floatValue() : null;
    }
}
