package com.factcheck.youtube.dto;

import java.util.List;

public record YoutubeAiCommentAnalysis(
        String text,
        int likes,
        String authorName,
        String authorId,
        String sentiment,
        double sentimentScore,
        int botScore,
        boolean isBot,
        List<String> botReasons
) {
}
