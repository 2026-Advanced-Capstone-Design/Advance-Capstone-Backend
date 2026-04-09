package com.factcheck.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 내부 오류입니다."),

    INVALID_YOUTUBE_URL(HttpStatus.BAD_REQUEST, "YOUTUBE_001", "유효하지 않은 유튜브 URL입니다."),
    UNSUPPORTED_YOUTUBE_URL_FORMAT(HttpStatus.BAD_REQUEST, "YOUTUBE_002", "지원하지 않는 유튜브 URL 형식입니다."),
    VIDEO_ID_NOT_FOUND(HttpStatus.BAD_REQUEST, "YOUTUBE_003", "videoId를 찾을 수 없습니다."),
    INVALID_VIDEO_ID(HttpStatus.BAD_REQUEST, "YOUTUBE_004", "유효하지 않은 videoId입니다.");

    private HttpStatus status;
    private String code;
    private String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
