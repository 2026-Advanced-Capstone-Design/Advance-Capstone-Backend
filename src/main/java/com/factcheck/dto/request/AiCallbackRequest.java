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
    private String status;          // "DONE" or "FAILED"

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

    @JsonProperty("source_balance")
    private Double sourceBalance;

    @JsonProperty("bias_score")
    private Double biasScore;

    @JsonProperty("total_score")
    private Integer totalScore;

    @JsonProperty("highlighted_sentences")
    private List<HighlightedSentence> highlightedSentences;

    @JsonProperty("sections")
    private List<SectionResult> sections;

    @Getter
    @NoArgsConstructor
    public static class SectionResult {
        @JsonProperty("topic")
        private String topic;          // 정치 | 경제 | 사회 | 국제 | 기타

        @JsonProperty("bias_label")
        private String biasLabel;      // progressive | conservative | neutral 등

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
        private String type;   // vocab | framing | citation | omission

        @JsonProperty("score")
        private Double score;
    }
}
