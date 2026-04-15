package com.factcheck.youtube.service;

import com.factcheck.youtube.dto.YoutubeAiAnalysisRequest;
import com.factcheck.youtube.dto.YoutubeAiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class YoutubeAiAnalysisService {

    private final RestClient restClient;

    @Value("${ai.youtube.analysis-url}")
    private String analysisUrl;

    public YoutubeAiAnalysisResponse analyze(String videoId) {
        return restClient.post()
                .uri(analysisUrl)
                .body(new YoutubeAiAnalysisRequest(videoId))
                .retrieve()
                .body(YoutubeAiAnalysisResponse.class);
    }
}
