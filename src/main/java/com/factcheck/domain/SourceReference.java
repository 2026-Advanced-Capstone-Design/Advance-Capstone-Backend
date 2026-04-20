package com.factcheck.domain;

import com.factcheck.Enum.SourceType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "source_references")
@Getter
@NoArgsConstructor
public class SourceReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SOURCE_ID")
    private Long id;

    @Column(name = "source_name", length = 200)
    private String sourceName;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "credibility_score")
    private Float credibilityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESULT_ID")
    private AnalysisResult analysisResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTICLE_ID")
    private Article article;

    @Builder
    public SourceReference(String sourceName, String sourceUrl, Float credibilityScore,
                           SourceType sourceType, AnalysisResult analysisResult, Article article) {
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.credibilityScore = credibilityScore;
        this.sourceType = sourceType;
        this.analysisResult = analysisResult;
        this.article = article;
    }
}
