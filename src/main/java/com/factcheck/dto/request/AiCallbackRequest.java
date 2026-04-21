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
}
