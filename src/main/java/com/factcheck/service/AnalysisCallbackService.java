package com.factcheck.service;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.AnalysisResult;
import com.factcheck.domain.AnalysisSection;
import com.factcheck.domain.Article;
import com.factcheck.domain.SentenceAnalysis;
import com.factcheck.dto.request.AiCallbackRequest;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.repository.AnalysisResultRepository;
import com.factcheck.repository.AnalysisSectionRepository;
import com.factcheck.repository.ArticleRepository;
import com.factcheck.repository.SentenceAnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisCallbackService {

    private final ArticleRepository articleRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final AnalysisSectionRepository analysisSectionRepository;
    private final SentenceAnalysisRepository sentenceAnalysisRepository;
    private final AnalysisCacheWriter analysisCacheWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void handleCallback(AiCallbackRequest req) {
        Article article = articleRepository.findById(req.getArticleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        // 멱등성 가드: 이미 결과가 저장돼 있으면(=이전 콜백이 성공 처리됨) 중복 콜백이므로 조용히 무시한다.
        // 분산 환경에서 콜백은 유실·중복될 수 있어(at-least-once), 받는 쪽이 몇 번 와도 안전해야 한다.
        // 최종 방어선은 ARTICLE_ID UNIQUE(DB)이고, 이 가드는 정상 흐름에서 중복 INSERT/500을 예방한다.
        if (analysisResultRepository.existsByArticleId(req.getArticleId())) {
            log.info("중복 콜백 무시 (분석 결과 이미 존재): articleId={}", req.getArticleId());
            return;
        }

        if ("FAILED".equals(req.getStatus())) {
            article.updateStatus(ArticleStatus.FAILED);
            log.warn("AI 분석 실패: articleId={}, error={}", req.getArticleId(), req.getError());
            return;
        }

        AnalysisResult result = AnalysisResult.builder()
                .article(article)
                .title(req.getTopic())
                .keyFacts(toJson(req.getKeyFacts()))
                .keywords(toJson(req.getKeywords()))
                .factCheckReason(req.getFactCheckReason())
                .biasLabel(req.getBiasLabel())
                .biasConfidence(toFloat(req.getBiasConfidence()))
                .biasReason(req.getBiasReason())
                .emotionNeutrality(toFloat(req.getEmotionNeutrality()))
                .factRatio(toFloat(req.getFactRatio()))
                .biasScore(toFloat(req.getBiasScore()))
                .totalScore(req.getTotalScore())
                .cotEmotionReason(req.getCotEmotionReason())
                .build();

        analysisResultRepository.save(result);

        List<AiCallbackRequest.SectionResult> sections = req.getSections();
        if (sections != null && !sections.isEmpty()) {
            List<AnalysisSection> sectionEntities = sections.stream()
                    .map(sec -> AnalysisSection.builder()
                            .analysisResult(result)
                            .topic(sec.getTopic())
                            .biasLabel(sec.getBiasLabel())
                            .confidence(toFloat(sec.getConfidence()))
                            .reason(sec.getReason())
                            .step1BiasedExpressions(toJson(sec.getStep1BiasedExpressions()))
                            .step2NeutralExpressions(toJson(sec.getStep2NeutralExpressions()))
                            .step3Judgment(sec.getStep3Judgment())
                            .build())
                    .toList();
            analysisSectionRepository.saveAll(sectionEntities);
        }

        List<AiCallbackRequest.HighlightedSentence> highlighted = req.getHighlightedSentences();
        if (highlighted != null && !highlighted.isEmpty()) {
            List<SentenceAnalysis> sentenceEntities = highlighted.stream()
                    .map(hs -> SentenceAnalysis.builder()
                            .analysisResult(result)
                            .article(article)
                            .sentenceText(hs.getSentence())
                            .highlightType(hs.getType())
                            .highlightReason(hs.getReason())
                            .highlightScore(toFloat(hs.getScore()))
                            .build())
                    .toList();
            sentenceAnalysisRepository.saveAll(sentenceEntities);
        }

        article.updateStatus(ArticleStatus.DONE);

        // URL 입력 기사면 결과를 캐시에 기록한다. 캐싱 시점이 DONE 직후이므로 "완료된 분석"만 캐시에 들어가고,
        // 미완료(ANALYZING)·실패(FAILED) 기사는 절대 캐시로 서빙되지 않는다(결함 B 차단).
        // 캐시는 최적화라 실패해도 결과 저장/DONE 전이엔 영향이 없어야 하므로 별도 tx + best-effort로 처리.
        if (article.getSourceUrl() != null && !article.getSourceUrl().isBlank()) {
            String urlHash = DigestUtils.md5DigestAsHex(article.getSourceUrl().getBytes());
            try {
                analysisCacheWriter.cacheDoneResult(urlHash, article.getId());
            } catch (DataIntegrityViolationException e) {
                log.warn("캐시 저장 경합으로 스킵: articleId={}, urlHash={}", article.getId(), urlHash);
            }
        }

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
