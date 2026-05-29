package com.factcheck.feedback.controller;

import com.factcheck.common.response.ApiResponse;
import com.factcheck.feedback.dto.FeedbackCreateRequest;
import com.factcheck.feedback.dto.FeedbackResponse;
import com.factcheck.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @Valid @RequestBody FeedbackCreateRequest request
    ) {
        FeedbackResponse response = feedbackService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "피드백이 저장되었습니다."));
    }
}
