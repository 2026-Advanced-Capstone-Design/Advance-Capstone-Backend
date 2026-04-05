package com.factcheck.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 내부 오류입니다."),

    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE_001", "해당 기사를 찾을 수 없습니다."),
    RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE_002", "분석 결과가 아직 없습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ARTICLE_003", "이미지 업로드에 실패했습니다.");

    private HttpStatus status;
    private String code;
    private String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
