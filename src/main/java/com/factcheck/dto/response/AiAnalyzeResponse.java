package com.factcheck.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiAnalyzeResponse {

    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("article_id")
    private Long articleId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;
}
