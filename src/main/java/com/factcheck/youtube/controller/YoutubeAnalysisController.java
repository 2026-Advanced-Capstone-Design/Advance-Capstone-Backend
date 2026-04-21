package com.factcheck.youtube.controller;

import com.factcheck.common.response.ApiResponse;
import com.factcheck.youtube.dto.YoutubeAnalysisResultResponse;
import com.factcheck.youtube.dto.YoutubeAnalysisStartResponse;
import com.factcheck.youtube.dto.YoutubeCommentRequest;
import com.factcheck.youtube.service.YoutubeAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/youtube/analysis")
@RequiredArgsConstructor
public class YoutubeAnalysisController {

    private final YoutubeAnalysisService youtubeAnalysisService;

    @PostMapping
    public ResponseEntity<ApiResponse<YoutubeAnalysisStartResponse>> startAnalysis(
            @Valid @RequestBody YoutubeCommentRequest request
    ) {
        YoutubeAnalysisStartResponse response = youtubeAnalysisService.startAnalysis(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "분석 요청이 접수되었습니다."));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ApiResponse<YoutubeAnalysisResultResponse>> getAnalysis(
            @PathVariable String requestId
    ) {
        YoutubeAnalysisResultResponse response = youtubeAnalysisService.getAnalysis(requestId);
        return ResponseEntity.ok(ApiResponse.ok(response, "분석 요청 조회 성공"));
    }
}