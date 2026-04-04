package com.factcheck.domain;

import com.factcheck.Enum.FactOrOpinion;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sentence_analyses")
@Getter
@NoArgsConstructor
public class SentenceAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SENTENCE_ID")
    private Long id;

    @Column(name = "sentence_index")
    private Integer sentenceIndex;

    @Column(name = "sentence_text", columnDefinition = "TEXT")
    private String sentenceText;

    @Enumerated(EnumType.STRING)
    @Column(name = "fact_or_opinion")
    private FactOrOpinion factOrOpinion;

    @Column(name = "fact_confidence")
    private Float factConfidence;

    @Column(name = "emotion_score")
    private Float emotionScore;

    @Column(name = "bias_score")
    private Float biasScore;

    @Column(name = "is_highlighted")
    private Boolean isHighlighted;

    @Column(name = "highlight_reason", columnDefinition = "TEXT")
    private String highlightReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESULT_ID")
    private AnalysisResult analysisResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTICLE_ID")
    private Article article;

    @Builder
    public SentenceAnalysis(Integer sentenceIndex, String sentenceText, FactOrOpinion factOrOpinion,
                            Float factConfidence, Float emotionScore, Float biasScore,
                            Boolean isHighlighted, String highlightReason,
                            AnalysisResult analysisResult, Article article) {
        this.sentenceIndex = sentenceIndex;
        this.sentenceText = sentenceText;
        this.factOrOpinion = factOrOpinion;
        this.factConfidence = factConfidence;
        this.emotionScore = emotionScore;
        this.biasScore = biasScore;
        this.isHighlighted = isHighlighted;
        this.highlightReason = highlightReason;
        this.analysisResult = analysisResult;
        this.article = article;
    }
}
