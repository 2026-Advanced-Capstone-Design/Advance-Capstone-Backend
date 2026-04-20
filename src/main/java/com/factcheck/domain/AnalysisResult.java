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

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "emotion_neutrality")
    private Float emotionNeutrality;

    @Column(name = "fact_ratio")
    private Float factRatio;

    @Column(name = "source_balance")
    private Float sourceBalance;

    @Column(name = "bias_score")
    private Float biasScore;

    @Column(name = "bias_direction", length = 20)
    private String biasDirection;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "spectrum_label", length = 50)
    private String spectrumLabel;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "bia_sentence", columnDefinition = "JSON")
    private String biaSentence;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTICLE_ID")
    private Article article;

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SentenceAnalysis> sentenceAnalyses = new ArrayList<>();

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourceReference> sourceReferences = new ArrayList<>();

    @Builder
    public AnalysisResult(Integer totalScore, Float emotionNeutrality, Float factRatio,
                          Float sourceBalance, Float biasScore, String biasDirection,
                          String title, String summary, String spectrumLabel,
                          String biaSentence, Article article) {
        this.totalScore = totalScore;
        this.emotionNeutrality = emotionNeutrality;
        this.factRatio = factRatio;
        this.sourceBalance = sourceBalance;
        this.biasScore = biasScore;
        this.biasDirection = biasDirection;
        this.title = title;
        this.summary = summary;
        this.spectrumLabel = spectrumLabel;
        this.biaSentence = biaSentence;
        this.article = article;
        this.analyzedAt = LocalDateTime.now();
    }
}
