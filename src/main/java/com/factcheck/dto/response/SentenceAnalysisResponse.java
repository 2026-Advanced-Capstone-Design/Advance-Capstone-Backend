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
    private String highlightReason;   // vocab | framing | citation | omission
    private String highlightDetails;  // 강조된 단어

    public SentenceAnalysisResponse(SentenceAnalysis s) {
        this.sentenceIndex    = s.getSentenceIndex();
        this.sentenceText     = s.getSentenceText();
        this.factOrOpinion    = s.getFactOrOpinion();
        this.factConfidence   = s.getFactConfidence();
        this.emotionScore     = s.getEmotionScore();
        this.biasScore        = s.getBiasScore();
        this.isHighlighted    = s.getIsHighlighted();
        this.highlightReason  = s.getHighlightReason();
        this.highlightDetails = s.getHighlight_details();
    }
}
