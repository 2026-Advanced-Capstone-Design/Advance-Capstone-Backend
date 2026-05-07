package com.factcheck.youtube.dto;

import com.factcheck.youtube.entity.YoutubeAnalysisStatus;

public record YoutubeAnalysisStartResponse(
        String requestId,
        String youtubeId,
        YoutubeAnalysisStatus status
) {
}
