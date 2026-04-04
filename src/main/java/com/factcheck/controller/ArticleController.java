package com.factcheck.controller;

import com.factcheck.common.response.ApiResponse;
import com.factcheck.dto.response.AnalyzeResponse;
import com.factcheck.dto.response.AnalysisResultResponse;
import com.factcheck.dto.response.AnalysisStatusResponse;
import com.factcheck.service.ArticleService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 텍스트 분석 요청
     * POST /api/v1/articles/analyze/text
     * Content-Type: application/json
     * Body: { "text": "기사 본문..." }
     */
    @PostMapping(value = "/analyze/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AnalyzeResponse>> analyzeText(
            @RequestBody TextRequest request) {
        AnalyzeResponse response = articleService.submitText(request.getText());
        return ResponseEntity.ok(ApiResponse.ok(response, "텍스트 분석 요청이 접수되었습니다."));
    }

    /**
     * URL 분석 요청
     * POST /api/v1/articles/analyze/url
     * Content-Type: application/json
     * Body: { "url": "https://..." }
     */
    @PostMapping(value = "/analyze/url", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AnalyzeResponse>> analyzeUrl(
            @RequestBody UrlRequest request) {
        AnalyzeResponse response = articleService.submitUrl(request.getUrl());
        return ResponseEntity.ok(ApiResponse.ok(response, "URL 분석 요청이 접수되었습니다."));
    }

    /**
     * 이미지 분석 요청
     * POST /api/v1/articles/analyze/image
     * Content-Type: multipart/form-data
     * Form: image (파일)
     */
    @PostMapping(value = "/analyze/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AnalyzeResponse>> analyzeImage(
            @RequestParam("image") MultipartFile image) {
        AnalyzeResponse response = articleService.submitImage(image);
        return ResponseEntity.ok(ApiResponse.ok(response, "이미지 분석 요청이 접수되었습니다."));
    }

    /**
     * 분석 상태 조회 (프론트 폴링용)
     * GET /api/v1/articles/{id}/status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AnalysisStatusResponse>> getStatus(
            @PathVariable Long id) {
        AnalysisStatusResponse response = articleService.getStatus(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "분석 상태 조회 성공"));
    }

    /**
     * 분석 결과 조회
     * GET /api/v1/articles/{id}/result
     */
    @GetMapping("/{id}/result")
    public ResponseEntity<ApiResponse<AnalysisResultResponse>> getResult(
            @PathVariable Long id) {
        AnalysisResultResponse response = articleService.getResult(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "분석 결과 조회 성공"));
    }

    @Getter
    @NoArgsConstructor
    public static class TextRequest {
        private String text;
    }

    @Getter
    @NoArgsConstructor
    public static class UrlRequest {
        private String url;
    }
}
