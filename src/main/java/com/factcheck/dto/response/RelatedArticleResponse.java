package com.factcheck.dto.response;

import lombok.Getter;

@Getter
public class RelatedArticleResponse {

    private String title;
    private String link;
    private String description;
    private String pubDate;

    public RelatedArticleResponse(String title, String link, String description, String pubDate) {
        this.title       = title.replaceAll("<[^>]*>", "");
        this.link        = link;
        this.description = description.replaceAll("<[^>]*>", "");
        this.pubDate     = pubDate;
    }
}
