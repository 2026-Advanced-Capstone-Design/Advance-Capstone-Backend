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

    @JsonProperty("compressed_text")
    private String compressedText;

    @JsonProperty("keywords")
    private List<String> keywords;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("bias_label")
    private String biasLabel;

    @JsonProperty("bias_confidence")
    private Double biasConfidence;

    @JsonProperty("bias_reason")
    private String biasReason;

    @JsonProperty("sections")
    private List<SectionResult> sections;

    @JsonProperty("highlighted_sentences")
    private List<HighlightedSentence> highlightedSentences;

    @JsonProperty("bias_direction")
    private String biasDirection;

    @JsonProperty("emotion_neutrality")
    private Double emotionNeutrality;

    @JsonProperty("fact_ratio")
    private Double factRatio;

    @JsonProperty("fact_ratio_source")
    private Double factRatioSource;

    @JsonProperty("bias_score")
    private Double biasScore;

    @JsonProperty("total_score")
    private Integer totalScore;

    @JsonProperty("cot_emotion_reason")
    private String cotEmotionReason;

    @JsonProperty("cot_fact_ratio_reason")
    private String cotFactRatioReason;

    @JsonProperty("fact_check_results")
    private List<FactCheckItem> factCheckResults;

    @Getter
    @NoArgsConstructor
    public static class FactCheckItem {
        @JsonProperty("fact")
        private String fact;

        @JsonProperty("found")
        private Boolean found;

        @JsonProperty("rating")
        private String rating;

        @JsonProperty("score")
        private Double score;

        @JsonProperty("title")
        private String title;

        @JsonProperty("publisher")
        private String publisher;

        @JsonProperty("url")
        private String url;
    }

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

    @Getter
    @NoArgsConstructor
    public static class HighlightedSentence {
        @JsonProperty("sentence")
        private String sentence;

        @JsonProperty("type")
        private String type;

        @JsonProperty("score")
        private Double score;

        @JsonProperty("reason")
        private String reason;
    }
}
