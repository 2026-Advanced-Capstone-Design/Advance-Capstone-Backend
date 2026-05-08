package com.factcheck.youtube.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "youtube_analysis_request")
public class YoutubeAnalysisRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(name = "youtube_url", nullable = false, length = 1000)
    private String youtubeUrl;

    @Column(name = "youtube_id", nullable = false, length = 20)
    private String youtubeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private YoutubeAnalysisStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected YoutubeAnalysisRequest() {
    }

    private YoutubeAnalysisRequest(String youtubeUrl, String youtubeId) {
        this.requestId = UUID.randomUUID().toString();
        this.youtubeUrl = youtubeUrl;
        this.youtubeId = youtubeId;
        this.status = YoutubeAnalysisStatus.PENDING;
    }

    public static YoutubeAnalysisRequest create(String youtubeUrl, String youtubeId) {
        return new YoutubeAnalysisRequest(youtubeUrl, youtubeId);
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

    public void markProcessing() {
        this.status = YoutubeAnalysisStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void markCompleted() {
        this.status = YoutubeAnalysisStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = YoutubeAnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public YoutubeAnalysisStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
