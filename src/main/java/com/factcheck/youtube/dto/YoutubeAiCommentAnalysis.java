package com.factcheck.youtube.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record YoutubeAiCommentAnalysis(
        String text,
        int likes,
        @JsonProperty("author_name")
        String authorName,
        @JsonProperty("author_id")
        String authorId,
        String sentiment,
        @JsonProperty("sentiment_score")
        double sentimentScore,
        @JsonProperty("bot_score")
        int botScore,
        @JsonProperty("is_bot")
        boolean isBot,
        @JsonProperty("bot_reasons")
        List<String> botReasons
) {
}
