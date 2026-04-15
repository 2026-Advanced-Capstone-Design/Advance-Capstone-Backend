package com.factcheck.youtube.service;

import com.factcheck.youtube.dto.YoutubeAnalysisStartResponse;
import com.factcheck.youtube.dto.YoutubeCommentRequest;
import com.factcheck.youtube.entity.YoutubeAnalysisRequest;
import com.factcheck.youtube.repository.YoutubeAnalysisRequestRepository;
import com.factcheck.youtube.util.YoutubeUrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YoutubeAnalysisService {

    private final YoutubeAnalysisRequestRepository youtubeAnalysisRequestRepository;

    @Transactional
    public YoutubeAnalysisStartResponse startAnalysis(YoutubeCommentRequest request) {
        String youtubeId = YoutubeUrlUtils.extractVideoId(request.youtubeUrl());
        YoutubeAnalysisRequest analysisRequest = YoutubeAnalysisRequest.create(
                request.youtubeUrl(),
                youtubeId
        );

        YoutubeAnalysisRequest savedRequest = youtubeAnalysisRequestRepository.save(analysisRequest);
        return new YoutubeAnalysisStartResponse(
                savedRequest.getRequestId(),
                savedRequest.getYoutubeId(),
                savedRequest.getStatus()
        );
    }
}
