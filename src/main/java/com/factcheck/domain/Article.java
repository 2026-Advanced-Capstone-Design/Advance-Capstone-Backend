package com.factcheck.domain;


import com.factcheck.Enum.ArticleStatus;
import com.factcheck.Enum.InputType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.web.WebProperties;

import javax.annotation.processing.Generated;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InputType inputType;

    @Column(length = 500)
    private String title;

    @Column(length = 500)
    private String originalText;

    @Column(length = 2000)
    private String sourceURL;

    @Column(length = 2000)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    private ArticleStatus articleStatus;

    private LocalDateTime createdAt;

    @PrePersist
    public void beforePersist(){
        this.createdAt = LocalDateTime.now();
    }




}
