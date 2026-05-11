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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
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
            log.info("[유튜브 분석] 분석 처리 시작 requestId={}, youtubeId={}",
                    analysisRequest.getRequestId(), analysisRequest.getYoutubeId());

            YoutubeAiAnalysisResponse aiResponse = youtubeAiAnalysisService.analyze(analysisRequest.getYoutubeId());
            if (aiResponse == null) {
                throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID);
            }
            log.info("[유튜브 분석] AI 응답 수신 requestId={}, videoTitle={}, total={}",
                    analysisRequest.getRequestId(), aiResponse.videoTitle(), aiResponse.total());

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
                    aiResponse.positiveSummary(),
                    aiResponse.negativeSummary(),
                    aiResponse.neutralSummary(),
                    aiResponse.specialNotes(),
                    toCommentsJson(aiResponse)
            );
            youtubeAnalysisResultRepository.save(result);
            log.info("[유튜브 분석] 분석 결과 저장 완료 requestId={}, youtubeId={}",
                    analysisRequest.getRequestId(), analysisRequest.getYoutubeId());

            analysisRequest.markCompleted();
            youtubeAnalysisRequestRepository.save(analysisRequest);
            log.info("[유튜브 분석] 분석 완료 requestId={}, youtubeId={}",
                    analysisRequest.getRequestId(), analysisRequest.getYoutubeId());
        } catch (BusinessException e) {
            log.error("[유튜브 분석] 비즈니스 예외 발생 requestId={}, youtubeId={}, error={}",
                    analysisRequest.getRequestId(), analysisRequest.getYoutubeId(), e.getMessage(), e);
            analysisRequest.markFailed(e.getMessage());
            youtubeAnalysisRequestRepository.save(analysisRequest);
        } catch (DataAccessException e) {
            log.error("[유튜브 분석] DB 처리 실패 requestId={}, youtubeId={}, error={}",
                    analysisRequest.getRequestId(), analysisRequest.getYoutubeId(), e.getMessage(), e);
            analysisRequest.markFailed(ErrorCode.ANALYSIS_RESULT_SAVE_FAILED.getMessage());
            youtubeAnalysisRequestRepository.save(analysisRequest);
        } catch (Exception e) {
            log.error("[유튜브 분석] 알 수 없는 예외 발생 requestId={}, youtubeId={}, error={}",
                    analysisRequest.getRequestId(), analysisRequest.getYoutubeId(), e.getMessage(), e);
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
