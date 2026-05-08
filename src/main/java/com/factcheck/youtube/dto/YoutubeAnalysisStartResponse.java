package com.factcheck.youtube.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.factcheck.youtube.entity.YoutubeAnalysisStatus;

public record YoutubeAnalysisStartResponse(
        @JsonProperty("request_id")
        String requestId,
        @JsonProperty("youtube_id")
        String youtubeId,
        YoutubeAnalysisStatus status
) {
}
