package com.factcheck.domain;

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

    @Column(name = "sentence_text", columnDefinition = "TEXT")
    private String sentenceText;

    @Column(name = "highlight_type", length = 30)
    private String highlightType;

    @Column(name = "highlight_reason", columnDefinition = "TEXT")
    private String highlightReason;

    @Column(name = "highlight_score")
    private Float highlightScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESULT_ID")
    private AnalysisResult analysisResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTICLE_ID")
    private Article article;

    @Builder
    public SentenceAnalysis(String sentenceText, String highlightType, String highlightReason,
                            Float highlightScore, AnalysisResult analysisResult, Article article) {
        this.sentenceText    = sentenceText;
        this.highlightType   = highlightType;
        this.highlightReason = highlightReason;
        this.highlightScore  = highlightScore;
        this.analysisResult  = analysisResult;
        this.article         = article;
    }
}
