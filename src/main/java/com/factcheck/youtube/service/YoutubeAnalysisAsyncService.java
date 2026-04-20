package com.factcheck.youtube.service;

import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.youtube.dto.YoutubeAiAnalysisResponse;
import com.factcheck.youtube.entity.YoutubeAnalysisRequest;
import com.factcheck.youtube.entity.YoutubeAnalysisResult;
import com.factcheck.youtube.repository.YoutubeAnalysisRequestRepository;
import com.factcheck.youtube.repository.YoutubeAnalysisResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YoutubeAnalysisAsyncService {

    private final YoutubeAnalysisRequestRepository youtubeAnalysisRequestRepository;
    private final YoutubeAnalysisResultRepository youtubeAnalysisResultRepository;
    private final YoutubeAiAnalysisService youtubeAiAnalysisService;
    private final ObjectMapper objectMapper;

    @Async
    public void analyze(Long analysisRequestId) {
        YoutubeAnalysisRequest analysisRequest = youtubeAnalysisRequestRepository.findById(analysisRequestId)
                .orElseThrow();

        try {
            analysisRequest.markProcessing();
            youtubeAnalysisRequestRepository.save(analysisRequest);

            YoutubeAiAnalysisResponse aiResponse = youtubeAiAnalysisService.analyze(analysisRequest.getYoutubeId());
            if (aiResponse == null) {
                throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID);
            }

            YoutubeAnalysisResult result = YoutubeAnalysisResult.create(
                    analysisRequest,
                    aiResponse.videoTitle(),
                    aiResponse.channelName(),
                    aiResponse.viewCount(),
                    aiResponse.publishedAt(),
                    aiResponse.videoCommentCount(),
                    aiResponse.total(),
                    aiResponse.positive(),
                    aiResponse.negative(),
                    aiResponse.neutral(),
                    aiResponse.positivePct(),
                    aiResponse.negativePct(),
                    aiResponse.neutralPct(),
                    aiResponse.botCount(),
                    aiResponse.botPct(),
                    aiResponse.summary(),
                    toCommentsJson(aiResponse)
            );
            youtubeAnalysisResultRepository.save(result);

            analysisRequest.markCompleted();
            youtubeAnalysisRequestRepository.save(analysisRequest);
        } catch (BusinessException e) {
            analysisRequest.markFailed(e.getMessage());
            youtubeAnalysisRequestRepository.save(analysisRequest);
        } catch (DataAccessException e) {
            analysisRequest.markFailed(ErrorCode.ANALYSIS_RESULT_SAVE_FAILED.getMessage());
            youtubeAnalysisRequestRepository.save(analysisRequest);
        } catch (Exception e) {
            analysisRequest.markFailed(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
            youtubeAnalysisRequestRepository.save(analysisRequest);
        }
    }

    private String toCommentsJson(YoutubeAiAnalysisResponse aiResponse) {
        try {
            return objectMapper.writeValueAsString(
                    aiResponse.comments() == null ? List.of() : aiResponse.comments()
            );
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }
}
