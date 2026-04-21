package com.factcheck.youtube.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record YoutubeAiAnalysisResponse(
        @JsonProperty("video_title")
        String videoTitle,
        @JsonProperty("channel_name")
        String channelName,
        @JsonProperty("view_count")
        long viewCount,
        @JsonProperty("published_at")
        String publishedAt,
        @JsonProperty("video_comment_count")
        String videoCommentCount,
        int total,
        int positive,
        int negative,
        int neutral,
        @JsonProperty("positive_pct")
        double positivePct,
        @JsonProperty("negative_pct")
        double negativePct,
        @JsonProperty("neutral_pct")
        double neutralPct,
        @JsonProperty("bot_count")
        int botCount,
        @JsonProperty("bot_pct")
        double botPct,
        String summary,
        List<YoutubeAiCommentAnalysis> comments
) {
}
