package com.factcheck.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 내부 오류입니다."),

    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE_001", "해당 기사를 찾을 수 없습니다."),
    RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "ARTICLE_002", "분석 결과가 아직 없습니다."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ARTICLE_003", "이미지 업로드에 실패했습니다."),

    AI_SERVER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI_001", "AI 분석 서버에 연결할 수 없습니다."),
    AI_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_002", "AI 분석 처리 중 오류가 발생했습니다."),

    INVALID_YOUTUBE_URL(HttpStatus.BAD_REQUEST, "YOUTUBE_001", "유효하지 않은 유튜브 URL입니다."),
    UNSUPPORTED_YOUTUBE_URL_FORMAT(HttpStatus.BAD_REQUEST, "YOUTUBE_002", "지원하지 않는 유튜브 URL 형식입니다."),
    VIDEO_ID_NOT_FOUND(HttpStatus.BAD_REQUEST, "YOUTUBE_003", "videoId를 찾을 수 없습니다."),
    INVALID_VIDEO_ID(HttpStatus.BAD_REQUEST, "YOUTUBE_004", "유효하지 않은 videoId입니다."),

    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "ANALYSIS_001", "분석 요청을 찾을 수 없습니다."),
    AI_SERVER_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "ANALYSIS_002", "AI 서버에 분석 요청을 보내지 못했습니다. 잠시 후 다시 시도해주세요."),
    AI_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "ANALYSIS_003", "AI 분석 결과 형식이 올바르지 않습니다."),
    ANALYSIS_RESULT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_004", "분석 결과 저장 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}