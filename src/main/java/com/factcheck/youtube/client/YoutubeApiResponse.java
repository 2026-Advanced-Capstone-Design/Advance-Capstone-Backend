package com.factcheck.youtube.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YoutubeApiResponse(
        List<Item> items
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(ThreadSnippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ThreadSnippet(
            int totalReplyCount,
            TopLevelComment topLevelComment
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopLevelComment(CommentSnippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentSnippet(
            String textOriginal,
            String authorDisplayName,
            int likeCount,
            String publishedAt
    ) {}
}
