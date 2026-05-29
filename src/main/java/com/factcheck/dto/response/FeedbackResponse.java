package com.factcheck.dto.response;

import com.factcheck.Enum.FeedbackType;

public record FeedbackResponse(
        Long id,
        Long resultId,
        FeedbackType feedbackType,
        String comment
) {
}
