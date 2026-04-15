package com.factcheck.youtube.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_analysis_result")
public class YoutubeAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_request_id", nullable = false, unique = true)
    private YoutubeAnalysisRequest analysisRequest;

    @Column(name = "video_title", length = 500)
    private String videoTitle;

    @Column(name = "video_comment_count", length = 50)
    private String videoCommentCount;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int positive;

    @Column(nullable = false)
    private int negative;

    @Column(nullable = false)
    private int neutral;

    @Column(name = "positive_pct", nullable = false)
    private double positivePct;

    @Column(name = "negative_pct", nullable = false)
    private double negativePct;

    @Column(name = "neutral_pct", nullable = false)
    private double neutralPct;

    @Column(name = "bot_count", nullable = false)
    private int botCount;

    @Column(name = "bot_pct", nullable = false)
    private double botPct;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(name = "comments_json", nullable = false, columnDefinition = "LONGTEXT")
    private String commentsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected YoutubeAnalysisResult() {
    }

    private YoutubeAnalysisResult(
            YoutubeAnalysisRequest analysisRequest,
            String videoTitle,
            String videoCommentCount,
            int total,
            int positive,
            int negative,
            int neutral,
            double positivePct,
            double negativePct,
            double neutralPct,
            int botCount,
            double botPct,
            String summary,
            String commentsJson
    ) {
        this.analysisRequest = analysisRequest;
        this.videoTitle = videoTitle;
        this.videoCommentCount = videoCommentCount;
        this.total = total;
        this.positive = positive;
        this.negative = negative;
        this.neutral = neutral;
        this.positivePct = positivePct;
        this.negativePct = negativePct;
        this.neutralPct = neutralPct;
        this.botCount = botCount;
        this.botPct = botPct;
        this.summary = summary;
        this.commentsJson = commentsJson;
    }

    public static YoutubeAnalysisResult create(
            YoutubeAnalysisRequest analysisRequest,
            String videoTitle,
            String videoCommentCount,
            int total,
            int positive,
            int negative,
            int neutral,
            double positivePct,
            double negativePct,
            double neutralPct,
            int botCount,
            double botPct,
            String summary,
            String commentsJson
    ) {
        return new YoutubeAnalysisResult(
                analysisRequest,
                videoTitle,
                videoCommentCount,
                total,
                positive,
                negative,
                neutral,
                positivePct,
                negativePct,
                neutralPct,
                botCount,
                botPct,
                summary,
                commentsJson
        );
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public YoutubeAnalysisRequest getAnalysisRequest() {
        return analysisRequest;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public String getVideoCommentCount() {
        return videoCommentCount;
    }

    public int getTotal() {
        return total;
    }

    public int getPositive() {
        return positive;
    }

    public int getNegative() {
        return negative;
    }

    public int getNeutral() {
        return neutral;
    }

    public double getPositivePct() {
        return positivePct;
    }

    public double getNegativePct() {
        return negativePct;
    }

    public double getNeutralPct() {
        return neutralPct;
    }

    public int getBotCount() {
        return botCount;
    }

    public double getBotPct() {
        return botPct;
    }

    public String getSummary() {
        return summary;
    }

    public String getCommentsJson() {
        return commentsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
