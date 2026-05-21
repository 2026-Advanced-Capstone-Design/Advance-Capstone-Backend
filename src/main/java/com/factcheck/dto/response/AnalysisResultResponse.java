package com.factcheck.dto.response;

import com.factcheck.domain.AnalysisResult;
import com.factcheck.domain.AnalysisSection;
import com.factcheck.domain.SentenceAnalysis;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class AnalysisResultResponse {

    private Long articleId;
    private String articleTitle;
    private String originalText;
    private Long resultId;
    private Integer totalScore;
    private Indicators indicators;
    private BiasInfo bias;
    private SummaryInfo summary;
    private CotReasons cotReasons;
    private List<SectionInfo> sections;
    private List<SentenceInfo> sentences;
    private LocalDateTime analyzedAt;

    public AnalysisResultResponse(AnalysisResult result) {
        this.articleId    = result.getArticle().getId();
        this.articleTitle = result.getArticle().getTitle();
        this.originalText = result.getArticle().getOriginalText();
        this.resultId     = result.getId();
        this.totalScore   = result.getTotalScore();
        this.indicators   = new Indicators(result);
        this.bias         = new BiasInfo(result);
        this.summary      = new SummaryInfo(result);
        this.cotReasons   = new CotReasons(result);
        this.sections     = result.getSections().stream().map(SectionInfo::new).toList();
        this.sentences    = result.getSentences().stream().map(SentenceInfo::new).toList();
        this.analyzedAt   = result.getAnalyzedAt();
    }

    @Getter
    public static class Indicators {
        private Float factRatio;
        private Float emotionNeutrality;
        private Float biasScore;
        private String factCheckReason;

        public Indicators(AnalysisResult r) {
            this.factRatio         = r.getFactRatio();
            this.emotionNeutrality = r.getEmotionNeutrality();
            this.biasScore         = r.getBiasScore();
            this.factCheckReason   = r.getFactCheckReason();
        }
    }

    @Getter
    public static class BiasInfo {
        private String biasLabel;
        private Float biasConfidence;
        private String biasReason;

        public BiasInfo(AnalysisResult r) {
            this.biasLabel      = r.getBiasLabel();
            this.biasConfidence = r.getBiasConfidence();
            this.biasReason     = r.getBiasReason();
        }
    }

    @Getter
    public static class SummaryInfo {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        private String title;
        private List<String> keywords;
        private List<String> keyFacts;

        public SummaryInfo(AnalysisResult r) {
            this.title    = r.getTitle();
            this.keywords = parseList(r.getKeywords());
            this.keyFacts = parseList(r.getKeyFacts());
        }

        private static List<String> parseList(String json) {
            if (json == null || json.isBlank()) return List.of();
            try {
                return MAPPER.readValue(json, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                return List.of();
            }
        }
    }

    @Getter
    public static class CotReasons {
        private String emotionNeutrality;

        public CotReasons(AnalysisResult r) {
            this.emotionNeutrality = r.getCotEmotionReason();
        }
    }

    @Getter
    public static class SectionInfo {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        private String topic;
        private String biasLabel;
        private Float confidence;
        private String reason;
        private List<String> step1BiasedExpressions;
        private List<String> step2NeutralExpressions;
        private String step3Judgment;

        public SectionInfo(AnalysisSection s) {
            this.topic                  = s.getTopic();
            this.biasLabel              = s.getBiasLabel();
            this.confidence             = s.getConfidence();
            this.reason                 = s.getReason();
            this.step1BiasedExpressions = parseList(s.getStep1BiasedExpressions());
            this.step2NeutralExpressions = parseList(s.getStep2NeutralExpressions());
            this.step3Judgment          = s.getStep3Judgment();
        }

        private static List<String> parseList(String json) {
            if (json == null || json.isBlank()) return List.of();
            try {
                return MAPPER.readValue(json, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                return List.of();
            }
        }
    }

    @Getter
    public static class SentenceInfo {
        private String sentenceText;
        private String highlightType;
        private String highlightReason;
        private Float highlightScore;

        public SentenceInfo(SentenceAnalysis s) {
            this.sentenceText    = s.getSentenceText();
            this.highlightType   = s.getHighlightType();
            this.highlightReason = s.getHighlightReason();
            this.highlightScore  = s.getHighlightScore();
        }
    }
}
