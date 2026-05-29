package com.factcheck.feedback.dto;

import com.factcheck.feedback.domain.FeedbackType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeedbackCreateRequest(
        @NotNull(message = "분석 결과 ID를 입력해주세요.")
        Long resultId,

        @NotNull(message = "좋아요 또는 싫어요를 선택해주세요.")
        FeedbackType feedbackType,

        @Size(max = 1000, message = "코멘트는 1,000자 이하로 입력해주세요.")
        String comment
) {
}
