package com.factcheck.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_cache")
@Getter
@NoArgsConstructor
public class AnalysisCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CACHE_ID")
    private Long id;

    @Column(name = "url_hash", length = 64)
    private String urlHash;

    @Column(name = "hit_count")
    private Integer hitCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARTICLE_ID")
    private Article article;

    @Builder
    public AnalysisCache(String urlHash, Article article) {
        this.urlHash = urlHash;
        this.hitCount = 1;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(7);
        this.article = article;
    }

    public void incrementHitCount() {
        this.hitCount++;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
