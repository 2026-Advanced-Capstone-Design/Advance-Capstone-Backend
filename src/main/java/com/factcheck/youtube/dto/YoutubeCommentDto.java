package com.factcheck.youtube.dto;

public record YoutubeCommentDto(
        String author,
        String text,
        int likeCount,
        int replyCount,
        String publishedAt
) {
}
