package com.factcheck.dto.request;

import com.factcheck.Enum.InputType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/v1/articles/analyze 요청 DTO
 *
 * 전송 방식: multipart/form-data
 * - input_type: TEXT | IMAGE | URL (필수)
 * - text:  TEXT 타입일 때 기사 본문 (선택)
 * - url:   URL  타입일 때 기사 주소 (선택)
 * - image: IMAGE 타입일 때 이미지 파일 → Controller에서 MultipartFile로 직접 수신
 */
@Getter
@NoArgsConstructor
public class AnalysisRequest {

    // 이 부분은 프론트에서 누르면 post 한것을 받는 형식으로
    @NotNull(message = "input_type은 필수입니다. (TEXT / IMAGE / URL)")
    private InputType inputType;

    private String text;

    private String url;
}
