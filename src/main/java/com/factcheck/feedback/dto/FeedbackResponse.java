package com.factcheck.feedback.dto;

import com.factcheck.feedback.domain.FeedbackType;

public record FeedbackResponse(
        Long id,
        Long resultId,
        FeedbackType feedbackType,
        String comment
) {
}
