package com.factcheck.controller;

import com.factcheck.common.response.ApiResponse;
import com.factcheck.dto.request.AiCallbackRequest;
import com.factcheck.service.AnalysisCallbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Flask AI 엔진이 호출하는 내부 전용 API.
 * 외부에서는 호출하지 않습니다.
 */
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalController {

    private final AnalysisCallbackService analysisCallbackService;

    /**
     * Flask 분석 완료 콜백
     * POST /api/v1/internal/callback
     */
    @PostMapping("/callback")
    public ResponseEntity<ApiResponse<Void>> callback(@RequestBody AiCallbackRequest request) {
        analysisCallbackService.handleCallback(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "콜백 처리 완료"));
    }
}
