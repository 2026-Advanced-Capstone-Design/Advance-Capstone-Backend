package com.factcheck.global.exception;

import com.factcheck.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        Map<String, Object> body = new HashMap<>();
        body.put("code", errorCode.getCode());
        body.put("messgae", errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(body, errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> validationErrors = new HashMap<>();

        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("code", ErrorCode.INVALID_INPUT.getCode());
        body.put("message", ErrorCode.INVALID_INPUT.getMessage());
        body.put("errors", validationErrors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(body, ErrorCode.INVALID_INPUT.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleException(Exception e) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        body.put("message", ErrorCode.INTERNAL_SERVER_ERROR.getMessage());

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(body, ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
