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

    @Column(name = "omission_neutrality")
    private Float omissionNeutrality;

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

    @Column(name = "sections", columnDefinition = "JSON")
    private String sections;

    @Column(name = "sources", columnDefinition = "JSON")
    private String sources;

    @Column(name = "fact_ratio_source", length = 10)
    private String factRatioSource;

    @Column(name = "factcheck_results", columnDefinition = "JSON")
    private String factcheckResults;

    @Column(name = "section_bias_score")
    private Float sectionBiasScore;

    @Column(name = "background", columnDefinition = "TEXT")
    private String background;

    @Column(name = "cot_vocab_reason", columnDefinition = "TEXT")
    private String cotVocabReason;

    @Column(name = "cot_framing_reason", columnDefinition = "TEXT")
    private String cotFramingReason;

    @Column(name = "cot_citation_reason", columnDefinition = "TEXT")
    private String cotCitationReason;

    @Column(name = "cot_omission_reason", columnDefinition = "TEXT")
    private String cotOmissionReason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTICLE_ID")
    private Article article;

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SentenceAnalysis> sentenceAnalyses = new ArrayList<>();

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourceReference> sourceReferences = new ArrayList<>();

    @Builder
    public AnalysisResult(Integer totalScore, Float emotionNeutrality, Float factRatio,
                          Float sourceBalance, Float omissionNeutrality, Float biasScore,
                          String biasDirection, String title, String summary, String spectrumLabel,
                          String biaSentence, String sections, String sources,
                          String factRatioSource, String factcheckResults, Float sectionBiasScore,
                          String background, String cotVocabReason, String cotFramingReason,
                          String cotCitationReason, String cotOmissionReason, Article article) {
        this.totalScore = totalScore;
        this.emotionNeutrality = emotionNeutrality;
        this.factRatio = factRatio;
        this.sourceBalance = sourceBalance;
        this.omissionNeutrality = omissionNeutrality;
        this.biasScore = biasScore;
        this.biasDirection = biasDirection;
        this.title = title;
        this.summary = summary;
        this.spectrumLabel = spectrumLabel;
        this.biaSentence = biaSentence;
        this.sections = sections;
        this.sources = sources;
        this.factRatioSource = factRatioSource;
        this.factcheckResults = factcheckResults;
        this.sectionBiasScore = sectionBiasScore;
        this.background = background;
        this.cotVocabReason = cotVocabReason;
        this.cotFramingReason = cotFramingReason;
        this.cotCitationReason = cotCitationReason;
        this.cotOmissionReason = cotOmissionReason;
        this.article = article;
        this.analyzedAt = LocalDateTime.now();
    }
}
