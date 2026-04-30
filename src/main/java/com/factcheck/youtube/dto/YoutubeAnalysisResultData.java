package com.factcheck.youtube.dto;

import java.util.List;

public record YoutubeAnalysisResultData(
        String videoTitle,
        String channelName,
        Long viewCount,
        String publishedAt,
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
        String positiveSummary,
        String negativeSummary,
        String neutralSummary,
        String specialNotes,
        List<YoutubeAiCommentAnalysis> comments
) {
}
