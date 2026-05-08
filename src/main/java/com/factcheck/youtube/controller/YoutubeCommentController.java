package com.factcheck.youtube.controller;

import com.factcheck.common.response.ApiResponse;
import com.factcheck.youtube.dto.YoutubeCommentRequest;
import com.factcheck.youtube.service.YoutubeCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeCommentController {

    private final YoutubeCommentService youtubeCommentService;

    @PostMapping("/comments")
    public ResponseEntity<ApiResponse<List<String>>> getComments(
            @Valid @RequestBody YoutubeCommentRequest request
    ) {
        List<String> response = youtubeCommentService.getComments(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "댓글 조회 성공"));
    }
}