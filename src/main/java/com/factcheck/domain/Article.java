package com.factcheck.domain;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.Enum.InputType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ARTICLE_ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false)
    private InputType inputType;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ArticleStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public Article(InputType inputType, String title, String originalText,
                   String sourceUrl, String imagePath) {
        this.inputType = inputType;
        this.title = title;
        this.originalText = originalText;
        this.sourceUrl = sourceUrl;
        this.imagePath = imagePath;
        this.status = ArticleStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void updateStatus(ArticleStatus status) {
        this.status = status;
    }
}
