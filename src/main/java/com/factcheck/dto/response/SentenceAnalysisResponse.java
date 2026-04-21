package com.factcheck.dto.response;

import com.factcheck.Enum.FactOrOpinion;
import com.factcheck.domain.SentenceAnalysis;
import lombok.Getter;

@Getter
public class SentenceAnalysisResponse {

    private Integer sentenceIndex;
    private String sentenceText;
    private FactOrOpinion factOrOpinion;
    private Float factConfidence;
    private Float emotionScore;
    private Float biasScore;
    private Boolean isHighlighted;
    private String highlightReason;

    public SentenceAnalysisResponse(SentenceAnalysis s) {
        this.sentenceIndex = s.getSentenceIndex();
        this.sentenceText = s.getSentenceText();
        this.factOrOpinion = s.getFactOrOpinion();
        this.factConfidence = s.getFactConfidence();
        this.emotionScore = s.getEmotionScore();
        this.biasScore = s.getBiasScore();
        this.isHighlighted = s.getIsHighlighted();
        this.highlightReason = s.getHighlightReason();
    }
}
