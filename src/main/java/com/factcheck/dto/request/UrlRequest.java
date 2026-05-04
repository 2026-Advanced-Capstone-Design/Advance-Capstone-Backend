package com.factcheck.dto.request;

import com.factcheck.domain.Article;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UrlRequest {

    @NotBlank(message = "URL을 입력해주세요.")
    @Pattern(
            regexp = "^https?://[\\w\\-]+(\\.[\\w\\-]+)+(/[\\w\\-./?%&=]*)?$",
            message = "올바른 URL 형식이 아닙니다. (http:// 또는 https://로 시작해야 합니다)"
    )
    private String url;

    public Article toEntity(String title, String processedText) {
        return Article.createFromUrl(this.url, title, processedText);
    }
}
