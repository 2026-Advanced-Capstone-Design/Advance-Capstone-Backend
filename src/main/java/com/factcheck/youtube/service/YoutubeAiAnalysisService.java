package com.factcheck.youtube.service;

import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.youtube.dto.YoutubeAiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeAiAnalysisService {

    private final RestClient restClient;

    @Value("${ai.youtube.analysis-url}")
    private String analysisUrl;

    public YoutubeAiAnalysisResponse analyze(String videoId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(analysisUrl)
                    .pathSegment(videoId)
                    .toUriString();

            log.info("[유튜브 AI] 분석 요청 시작 videoId={}, url={}", videoId, url);

            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(YoutubeAiAnalysisResponse.class);
        } catch (IllegalArgumentException | RestClientException e) {
            log.error("[유튜브 AI] 분석 요청 실패 videoId={}, analysisUrl={}, error={}",
                    videoId, analysisUrl, e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED);
        }
    }
}
