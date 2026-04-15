package com.factcheck.youtube.service;

import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.youtube.dto.YoutubeAnalysisResultData;
import com.factcheck.youtube.dto.YoutubeAnalysisResultResponse;
import com.factcheck.youtube.dto.YoutubeAnalysisStartResponse;
import com.factcheck.youtube.dto.YoutubeCommentRequest;
import com.factcheck.youtube.entity.YoutubeAnalysisRequest;
import com.factcheck.youtube.entity.YoutubeAnalysisResult;
import com.factcheck.youtube.repository.YoutubeAnalysisRequestRepository;
import com.factcheck.youtube.repository.YoutubeAnalysisResultRepository;
import com.factcheck.youtube.util.YoutubeUrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YoutubeAnalysisService {

    private final YoutubeAnalysisRequestRepository youtubeAnalysisRequestRepository;
    private final YoutubeAnalysisResultRepository youtubeAnalysisResultRepository;

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

    @Transactional(readOnly = true)
    public YoutubeAnalysisResultResponse getAnalysis(String requestId) {
        YoutubeAnalysisRequest analysisRequest = youtubeAnalysisRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));

        YoutubeAnalysisResultData result = youtubeAnalysisResultRepository.findByAnalysisRequest(analysisRequest)
                .map(this::toResultData)
                .orElse(null);

        return new YoutubeAnalysisResultResponse(
                analysisRequest.getRequestId(),
                analysisRequest.getYoutubeId(),
                analysisRequest.getStatus(),
                result,
                analysisRequest.getErrorMessage()
        );
    }

    private YoutubeAnalysisResultData toResultData(YoutubeAnalysisResult result) {
        return new YoutubeAnalysisResultData(
                result.getVideoTitle(),
                result.getVideoCommentCount(),
                result.getTotal(),
                result.getPositive(),
                result.getNegative(),
                result.getNeutral(),
                result.getPositivePct(),
                result.getNegativePct(),
                result.getNeutralPct(),
                result.getBotCount(),
                result.getBotPct(),
                result.getSummary(),
                result.getCommentsJson()
        );
    }
}
