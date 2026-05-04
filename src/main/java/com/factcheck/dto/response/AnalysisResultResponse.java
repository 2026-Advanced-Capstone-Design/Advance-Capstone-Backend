package com.factcheck.dto.response;

import com.factcheck.domain.AnalysisResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /api/v1/articles/{id}/result 응답 DTO
 */
@Getter
public class AnalysisResultResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long articleId;
    private Long resultId;
    private Integer totalScore;
    private Indicators indicators;
    private BiasInfo bias;
    private SummaryInfo summary;
    private List<SectionInfo> sections;
    private List<SentenceAnalysisResponse> sentences;
    private LocalDateTime analyzedAt;
    private String originalText;
    private String articleSources;
    private String factRatioSource;
    private Float  sectionBiasScore;
    private String background;
    private CotReasons cotReasons;

    public AnalysisResultResponse(AnalysisResult result) {
        this.articleId       = result.getArticle().getId();
        this.resultId        = result.getId();
        this.originalText    = result.getArticle().getOriginalText();
        this.sections        = parseSections(result.getSections());
        this.articleSources  = result.getSources();
        this.factRatioSource = result.getFactRatioSource();
        this.sectionBiasScore = result.getSectionBiasScore();
        this.background      = result.getBackground();
        this.cotReasons      = new CotReasons(result);
        this.totalScore      = result.getTotalScore();
        this.indicators      = new Indicators(result);
        this.bias            = new BiasInfo(result);
        this.summary         = new SummaryInfo(result);
        this.analyzedAt      = result.getAnalyzedAt();
        this.sentences       = result.getSentenceAnalyses().stream()
                .map(SentenceAnalysisResponse::new)
                .collect(Collectors.toList());
    }

    private static List<SectionInfo> parseSections(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return Collections.emptyList();
        try {
            return MAPPER.readValue(json, new TypeReference<List<SectionInfo>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class SectionInfo {
        @JsonProperty("topic")
        private String topic;

        @JsonProperty("step1_biased_expressions")
        private List<String> step1BiasedExpressions;

        @JsonProperty("step2_neutral_expressions")
        private List<String> step2NeutralExpressions;

        @JsonProperty("step3_judgment")
        private String step3Judgment;

        @JsonProperty("bias_label")
        private String biasLabel;

        @JsonProperty("confidence")
        private Double confidence;

        @JsonProperty("reason")
        private String reason;
    }

    @Getter
    public static class Indicators {
        private Float emotionNeutrality;
        private Float factRatio;
        private Float sourceBalance;
        private Float omissionNeutrality;
        private Float biasScore;

        public Indicators(AnalysisResult result) {
            this.emotionNeutrality  = result.getEmotionNeutrality();
            this.factRatio          = result.getFactRatio();
            this.sourceBalance      = result.getSourceBalance();
            this.omissionNeutrality = result.getOmissionNeutrality();
            this.biasScore          = result.getBiasScore();
        }
    }

    @Getter
    public static class BiasInfo {
        private String biasDirection;
        private String spectrumLabel;
        private String biasLabel;
        private Float  biasConfidence;
        private String biasReason;

        public BiasInfo(AnalysisResult result) {
            this.biasDirection  = result.getBiasDirection();
            this.spectrumLabel  = result.getSpectrumLabel();
            this.biasLabel      = result.getBiasLabel();
            this.biasConfidence = result.getBiasConfidence();
            this.biasReason     = result.getBiasReason();
        }
    }

    @Getter
    public static class SummaryInfo {
        private String title;
        private String content;
        private String keyFacts;
        private String keywords;

        public SummaryInfo(AnalysisResult result) {
            this.title    = result.getTitle();
            this.content  = result.getSummary();
            this.keyFacts = result.getKeyFacts();
            this.keywords = result.getKeywords();
        }
    }

    @Getter
    public static class CotReasons {
        private String vocab;
        private String framing;
        private String citation;
        private String omission;

        public CotReasons(AnalysisResult result) {
            this.vocab    = result.getCotVocabReason();
            this.framing  = result.getCotFramingReason();
            this.citation = result.getCotCitationReason();
            this.omission = result.getCotOmissionReason();
        }
    }
}
