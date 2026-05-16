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

    @Column(name = "highlight_score")
    private Float highlightScore;

    // fact | emotion | section_bias
    @Column(name = "highlight_type", length = 30)
    private String highlightType;

    @Column(name = "highlight_reason", columnDefinition = "TEXT")
    private String highlightReason;

    @Column
    private String highlight_details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESULT_ID")
    private AnalysisResult analysisResult;

    // 구분을 위한 article key가 필요로 하다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTICLE_ID")
    private Article article;

    @Builder
    public SentenceAnalysis(Integer sentenceIndex, String sentenceText, FactOrOpinion factOrOpinion,
                            Float factConfidence, Float emotionScore, Float highlightScore,
                            String highlightType, String highlightReason,
                            AnalysisResult analysisResult, Article article) {
        this.sentenceIndex = sentenceIndex;
        this.sentenceText = sentenceText;
        this.factOrOpinion = factOrOpinion;
        this.factConfidence = factConfidence;
        this.emotionScore = emotionScore;
        this.highlightScore = highlightScore;
        this.highlightType = highlightType;
        this.highlightReason = highlightReason;
        this.analysisResult = analysisResult;
        this.article = article;
    }
}
