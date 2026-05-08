package com.factcheck.youtube.service;

import com.factcheck.youtube.client.YoutubeApiResponse;
import com.factcheck.youtube.dto.YoutubeCommentRequest;
import com.factcheck.youtube.util.YoutubeUrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YoutubeCommentService {

    private static final String COMMENT_THREADS_URL =
            "https://www.googleapis.com/youtube/v3/commentThreads";

    private final RestClient restClient;

    @Value("${youtube.api.key}")
    private String apiKey;

    public List<String> getComments(YoutubeCommentRequest request) {
        String videoId = YoutubeUrlUtils.extractVideoId(request.youtubeUrl());

        String url = UriComponentsBuilder.fromHttpUrl(COMMENT_THREADS_URL)
                .queryParam("part", "snippet")
                .queryParam("videoId", videoId)
                .queryParam("maxResults", 100)
                .queryParam("order", "relevance")
                .queryParam("key", apiKey)
                .toUriString();

        YoutubeApiResponse apiResponse = restClient.get()
                .uri(url)
                .retrieve()
                .body(YoutubeApiResponse.class);

        if (apiResponse == null || apiResponse.items() == null) {
            return List.of();
        }

        return apiResponse.items().stream()
                .map(item -> item.snippet().topLevelComment().snippet().textOriginal())
                .toList();
    }
}
