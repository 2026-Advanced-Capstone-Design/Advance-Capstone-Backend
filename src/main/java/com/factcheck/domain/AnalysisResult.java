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

    @Column(name = "bias_label", length = 30)
    private String biasLabel;

    @Column(name = "bias_confidence")
    private Float biasConfidence;

    @Column(name = "bias_reason", columnDefinition = "TEXT")
    private String biasReason;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "spectrum_label", length = 50)
    private String spectrumLabel;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(name = "key_facts", columnDefinition = "JSON")
    private String keyFacts;

    @Column(name = "keywords", columnDefinition = "JSON")
    private String keywords;

    @Column(name = "sections", columnDefinition = "JSON")
    private String sections;

    @Column(name = "sources", columnDefinition = "JSON")
    private String sources;

    @Column(name = "fact_ratio_source", length = 10)
    private String factRatioSource;

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
    @JoinColumn(name = "ARTICLE_ID", unique = true)
    private Article article;

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SentenceAnalysis> sentenceAnalyses = new ArrayList<>();

    @OneToMany(mappedBy = "analysisResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourceReference> sourceReferences = new ArrayList<>();

    @Builder
    public AnalysisResult(Integer totalScore, Float emotionNeutrality, Float factRatio,
                          Float sourceBalance, Float omissionNeutrality, Float biasScore,
                          String biasDirection, String biasLabel, Float biasConfidence, String biasReason,
                          String title, String summary, String spectrumLabel,
                          String keyFacts, String keywords, String sections, String sources,
                          String factRatioSource, Float sectionBiasScore,
                          String background, String cotVocabReason, String cotFramingReason,
                          String cotCitationReason, String cotOmissionReason, Article article) {
        this.totalScore = totalScore;
        this.emotionNeutrality = emotionNeutrality;
        this.factRatio = factRatio;
        this.sourceBalance = sourceBalance;
        this.omissionNeutrality = omissionNeutrality;
        this.biasScore = biasScore;
        this.biasDirection  = biasDirection;
        this.biasLabel      = biasLabel;
        this.biasConfidence = biasConfidence;
        this.biasReason     = biasReason;
        this.title = title;
        this.summary = summary;
        this.spectrumLabel = spectrumLabel;
        this.keyFacts  = keyFacts;
        this.keywords  = keywords;
        this.sections = sections;
        this.sources = sources;
        this.factRatioSource = factRatioSource;
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
