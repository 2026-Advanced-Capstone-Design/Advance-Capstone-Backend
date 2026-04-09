package com.factcheck.youtube.dto;

import jakarta.validation.constraints.NotBlank;

public record YoutubeCommentRequest (
        @NotBlank(message = "url은 필수입니다.")
        String youtubeUrl
){
}