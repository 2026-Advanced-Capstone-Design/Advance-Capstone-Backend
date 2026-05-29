package com.factcheck.feedback.service;

import com.factcheck.domain.AnalysisResult;
import com.factcheck.domain.UserFeedback;
import com.factcheck.feedback.dto.FeedbackCreateRequest;
import com.factcheck.feedback.dto.FeedbackResponse;
import com.factcheck.feedback.repository.FeedbackRepository;
import com.factcheck.global.exception.BusinessException;
import com.factcheck.global.exception.ErrorCode;
import com.factcheck.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AnalysisResultRepository analysisResultRepository;

    @Transactional
    public FeedbackResponse create(FeedbackCreateRequest request) {
        AnalysisResult analysisResult = analysisResultRepository.findById(request.resultId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESULT_NOT_FOUND));

        UserFeedback feedback = UserFeedback.builder()
                .feedbackType(request.feedbackType())
                .comment(request.comment())
                .analysisResult(analysisResult)
                .build();

        UserFeedback savedFeedback = feedbackRepository.save(feedback);
        return new FeedbackResponse(
                savedFeedback.getId(),
                analysisResult.getId(),
                savedFeedback.getFeedbackType(),
                savedFeedback.getComment()
        );
    }
}
