package com.factcheck.youtube.dto;

import com.factcheck.youtube.entity.YoutubeAnalysisStatus;

public record YoutubeAnalysisResultResponse(
        String requestId,
        String youtubeId,
        YoutubeAnalysisStatus status,
        YoutubeAnalysisResultData result,
        String errorMessage
) {
}
