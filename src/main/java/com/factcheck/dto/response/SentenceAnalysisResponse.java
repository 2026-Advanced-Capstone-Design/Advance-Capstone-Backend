package com.factcheck.dto.response;

import com.factcheck.domain.SentenceAnalysis;
import lombok.Getter;

@Getter
public class SentenceAnalysisResponse {

    private String sentenceText;
    private String highlightType;
    private String highlightReason;
    private Float highlightScore;

    public SentenceAnalysisResponse(SentenceAnalysis s) {
        this.sentenceText    = s.getSentenceText();
        this.highlightType   = s.getHighlightType();
        this.highlightReason = s.getHighlightReason();
        this.highlightScore  = s.getHighlightScore();
    }
}
