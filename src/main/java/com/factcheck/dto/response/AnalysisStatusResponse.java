package com.factcheck.dto.response;

import com.factcheck.Enum.ArticleStatus;
import com.factcheck.domain.Article;
import lombok.Getter;

@Getter
public class AnalysisStatusResponse {

    private Long articleId;
    private ArticleStatus status;

    public AnalysisStatusResponse(Article article) {
        this.articleId = article.getId();
        this.status = article.getStatus();
    }
}
