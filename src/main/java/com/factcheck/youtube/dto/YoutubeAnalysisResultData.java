package com.factcheck.youtube.dto;

public record YoutubeAnalysisResultData(
        String videoTitle,
        String videoCommentCount,
        int total,
        int positive,
        int negative,
        int neutral,
        double positivePct,
        double negativePct,
        double neutralPct,
        int botCount,
        double botPct,
        String summary,
        String commentsJson
) {
}
