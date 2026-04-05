package com.factcheck.dto.response;

import com.factcheck.domain.Article;
import lombok.Getter;

/**
 * POST /api/v1/articles/analyze 응답 DTO
 * 명세 FR: 출력 → article_id (생성된 분석 요청 ID)
 */
@Getter
public class AnalyzeResponse {

    private Long articleId;

    public AnalyzeResponse(Article article) {
        this.articleId = article.getId();
    }
}
