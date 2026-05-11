package com.factcheck.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analysis_results")
@Getter
@NoArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RESULT_ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTICLE_ID")
    private Article article;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "compressed_text", columnDefinition = "TEXT")
    private String compressedText;

    @Column(name = "keywords", columnDefinition = "JSON")
    private String keywords;

    @Column(name = "fact_ratio_source")
    private Float factRatioSource;

    @Column(name = "bias_label", length = 50)
    private String biasLabel;

    @Column(name = "bias_confidence")
    private Float biasConfidence;

    @Column(name = "bias_reason", columnDefinition = "TEXT")
    private String biasReason;

    @Column(name = "bias_direction", length = 20)
    private String biasDirection;

    @Column(name = "emotion_neutrality")
    private Float emotionNeutrality;

    @Column(name = "fact_ratio")
    private Float factRatio;

    @Column(name = "bias_score")
    private Float biasScore;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "cot_emotion_reason", columnDefinition = "TEXT")
    private String cotEmotionReason;

    @Column(name = "cot_fact_ratio_reason", columnDefinition = "TEXT")
    private String cotFactRatioReason;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisSection> sections = new ArrayList<>();

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SentenceAnalysis> sentences = new ArrayList<>();

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FactCheckResult> factCheckResults = new ArrayList<>();

    @Builder
    public AnalysisResult(Article article, String title, String compressedText,
                          String keywords,
                          Float factRatioSource, String biasLabel, Float biasConfidence,
                          String biasReason, String biasDirection, Float emotionNeutrality,
                          Float factRatio, Float biasScore, Integer totalScore,
                          String cotEmotionReason, String cotFactRatioReason) {
        this.article = article;
        this.title = title;
        this.compressedText = compressedText;
        this.keywords = keywords;
        this.factRatioSource = factRatioSource;
        this.biasLabel = biasLabel;
        this.biasConfidence = biasConfidence;
        this.biasReason = biasReason;
        this.biasDirection = biasDirection;
        this.emotionNeutrality = emotionNeutrality;
        this.factRatio = factRatio;
        this.biasScore = biasScore;
        this.totalScore = totalScore;
        this.cotEmotionReason = cotEmotionReason;
        this.cotFactRatioReason = cotFactRatioReason;
        this.analyzedAt = LocalDateTime.now();
    }
}
