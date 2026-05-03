package com.factcheck.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiCallbackRequest {

    @JsonProperty("article_id")
    private Long articleId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("one_line_summary")
    private String oneLineSummary;

    @JsonProperty("key_facts")
    private List<String> keyFacts;

    @JsonProperty("keywords")
    private List<String> keywords;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("sentence_count")
    private Integer sentenceCount;

    @JsonProperty("sources")
    private List<String> sources;

    @JsonProperty("bias_label")
    private String biasLabel;

    @JsonProperty("bias_confidence")
    private Double biasConfidence;

    @JsonProperty("bias_reason")
    private String biasReason;

    @JsonProperty("bias_direction")
    private String biasDirection;

    @JsonProperty("spectrum_label")
    private String spectrumLabel;

    @JsonProperty("emotion_neutrality")
    private Double emotionNeutrality;

    @JsonProperty("fact_ratio")
    private Double factRatio;

    @JsonProperty("fact_ratio_source")
    private String factRatioSource;

    @JsonProperty("source_balance")
    private Double sourceBalance;

    @JsonProperty("omission_neutrality")
    private Double omissionNeutrality;

    @JsonProperty("bias_score")
    private Double biasScore;

    @JsonProperty("section_bias_score")
    private Double sectionBiasScore;

    @JsonProperty("total_score")
    private Integer totalScore;

    @JsonProperty("background")
    private String background;

    @JsonProperty("cot_vocab_reason")
    private String cotVocabReason;

    @JsonProperty("cot_framing_reason")
    private String cotFramingReason;

    @JsonProperty("cot_citation_reason")
    private String cotCitationReason;

    @JsonProperty("cot_omission_reason")
    private String cotOmissionReason;

    @JsonProperty("highlighted_sentences")
    private List<HighlightedSentence> highlightedSentences;

    @JsonProperty("sections")
    private List<SectionResult> sections;

    // ── 섹션별 편향 (CoT 3단계 포함) ──────────────────────────────────────
    @Getter
    @NoArgsConstructor
    public static class SectionResult {
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

    // ── 하이라이팅 문장 ────────────────────────────────────────────────────
    @Getter
    @NoArgsConstructor
    public static class HighlightedSentence {
        @JsonProperty("sentence")
        private String sentence;

        @JsonProperty("type")
        private String type;

        @JsonProperty("score")
        private Double score;
    }

}
