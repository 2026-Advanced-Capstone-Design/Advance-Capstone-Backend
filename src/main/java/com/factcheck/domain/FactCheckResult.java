package com.factcheck.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fact_check_results")
@Getter
@NoArgsConstructor
public class FactCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FACTCHECK_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESULT_ID")
    private AnalysisResult analysisResult;

    @Column(name = "fact", columnDefinition = "TEXT")
    private String fact;

    @Column(name = "found")
    private Boolean found;

    @Column(name = "rating", length = 100)
    private String rating;

    @Column(name = "score")
    private Float score;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "publisher", length = 200)
    private String publisher;

    @Column(name = "url", length = 2000)
    private String url;

    @Builder
    public FactCheckResult(AnalysisResult analysisResult, String fact, Boolean found,
                           String rating, Float score, String title,
                           String publisher, String url) {
        this.analysisResult = analysisResult;
        this.fact = fact;
        this.found = found;
        this.rating = rating;
        this.score = score;
        this.title = title;
        this.publisher = publisher;
        this.url = url;
    }
}
