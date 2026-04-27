package com.factcheck.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TextRequest {
    @NotBlank(message = "기사 본문을 입력해주세요.")
    @Size(min = 50, max = 50000, message = "기사 본문은 50자 이상 50,000자 이하이어야 합니다.")
    private String text;
}
