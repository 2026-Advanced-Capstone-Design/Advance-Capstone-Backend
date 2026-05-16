package com.factcheck.service;

import com.factcheck.domain.AnalysisResult;
import com.factcheck.dto.response.RelatedArticleResponse;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.repository.AnalysisResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsService {

    private static final String NAVER_NEWS_URL = "https://openapi.naver.com/v1/search/news.json";
    private static final int RESULT_COUNT = 3;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final AnalysisResultRepository analysisResultRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RelatedArticleResponse> getRelatedArticles(Long articleId) {
        AnalysisResult result = analysisResultRepository.findByArticleId(articleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ARTICLE_NOT_FOUND));

        String keyword = resolveKeyword(result);
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        log.info("관련 기사 검색 키워드: {}", keyword);
        return searchNews(keyword);
    }

    private String resolveKeyword(AnalysisResult result) {
        // topic(title 컬럼) 우선 사용
        if (result.getTitle() != null && !result.getTitle().isBlank()) {
            return result.getTitle();
        }
        // fallback: keywords[0]
        return extractTopKeyword(result.getKeywords());
    }

    private String extractTopKeyword(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isBlank() || keywordsJson.equals("[]")) return null;
        try {
            JsonNode node = objectMapper.readTree(keywordsJson);
            if (node.isArray() && node.size() > 0) {
                return node.get(0).asText();
            }
        } catch (Exception e) {
            log.warn("키워드 파싱 실패: {}", keywordsJson);
        }
        return null;
    }

    private List<RelatedArticleResponse> searchNews(String keyword) {
        String url = UriComponentsBuilder.fromHttpUrl(NAVER_NEWS_URL)
                .queryParam("query", keyword)
                .queryParam("display", RESULT_COUNT)
                .queryParam("sort", "date")
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.get("items");

            List<RelatedArticleResponse> articles = new ArrayList<>();
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    articles.add(new RelatedArticleResponse(
                            item.get("title").asText(),
                            item.get("link").asText(),
                            item.get("description").asText(),
                            item.get("pubDate").asText()
                    ));
                }
            }
            return articles;
        } catch (Exception e) {
            log.error("네이버 뉴스 검색 실패: keyword={}, error={}", keyword, e.getMessage());
            return List.of();
        }
    }
}
