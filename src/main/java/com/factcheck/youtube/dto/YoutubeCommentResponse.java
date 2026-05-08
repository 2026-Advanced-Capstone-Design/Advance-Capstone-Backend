package com.factcheck.youtube.dto;

import java.util.List;

public record YoutubeCommentResponse(
        String videoId,
        int totalCount,
        List<YoutubeCommentDto> comments
) {
}
