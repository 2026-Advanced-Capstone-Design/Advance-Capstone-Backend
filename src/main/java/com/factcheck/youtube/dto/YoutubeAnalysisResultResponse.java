package com.factcheck.youtube.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.factcheck.youtube.entity.YoutubeAnalysisStatus;

public record YoutubeAnalysisResultResponse(
        @JsonProperty("request_id")
        String requestId,
        @JsonProperty("youtube_id")
        String youtubeId,
        YoutubeAnalysisStatus status,
        YoutubeAnalysisResultData result,
        @JsonProperty("error_message")
        String errorMessage
) {
}
