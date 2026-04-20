package com.factcheck.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiAnalyzeRequest {

    @JsonProperty("article_id")
    private Long articleId;

    @JsonProperty("input_type")
    private String inputType;

    @JsonProperty("text")
    private String text;

    @JsonProperty("source_url")
    private String sourceUrl;
}
