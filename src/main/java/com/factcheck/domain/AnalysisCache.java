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

    // url_hash UNIQUE: URL 하나당 캐시 행은 최대 1개. 동시 요청 시 중복행/스탬피드로 인한
    // findByUrlHash NonUniqueResultException을 막는다(재분석 시엔 refresh로 갱신).
    @Column(name = "url_hash", length = 64, unique = true)
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

    /** 같은 URL을 재분석해 새 결과가 나왔을 때, 기존 캐시 행을 새 기사로 갱신하고 TTL을 리셋한다. */
    public void refresh(Article article) {
        this.article = article;
        this.hitCount = 1;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(7);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
