package com.factcheck.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "analysis_sections")
@Getter
@NoArgsConstructor
public class AnalysisSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SECTION_ID")
    private Long id;

    // result와의 mapping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESULT_ID")
    private AnalysisResult analysisResult;

    // section의 대한 주제
    @Column(name = "topic", length = 20)
    private String topic;

    @Column(name = "bias_label", length = 50)
    private String biasLabel;

    @Column(name = "confidence")
    private Float confidence;

    // 섹션 사유
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;
    
    @Column(name = "step1_biased_expressions", columnDefinition = "JSON")
    private String step1BiasedExpressions;

    @Column(name = "step2_neutral_expressions", columnDefinition = "JSON")
    private String step2NeutralExpressions;

    @Column(name = "step3_judgment", columnDefinition = "TEXT")
    private String step3Judgment;

    @Builder
    public AnalysisSection(AnalysisResult analysisResult, String topic, String biasLabel,
                           Float confidence, String reason, String step1BiasedExpressions,
                           String step2NeutralExpressions, String step3Judgment) {
        this.analysisResult = analysisResult;
        this.topic = topic;
        this.biasLabel = biasLabel;
        this.confidence = confidence;
        this.reason = reason;
        this.step1BiasedExpressions = step1BiasedExpressions;
        this.step2NeutralExpressions = step2NeutralExpressions;
        this.step3Judgment = step3Judgment;
    }
}
