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

    //하이라이트 대상인지 여부를 나타내는 문장(true인 문장만 강조)
    @Column(name = "is_highlighted")
    private Boolean isHighlighted;

    // 왜 주못을 해야 되는지
    @Column(name = "highlight_reason", columnDefinition = "TEXT")
    private String highlightReason;

    // 어떤 단어에 색을 칠할것인가...?
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
