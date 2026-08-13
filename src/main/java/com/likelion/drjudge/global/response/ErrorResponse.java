package com.likelion.drjudge.global.response;

import com.likelion.drjudge.global.exception.ErrorCode;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String errorCode,
        String message,
        String path,
        String traceId,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(status, errorCode, message, path, null, LocalDateTime.now());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String path, String traceId) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                message,
                path,
                traceId,
                LocalDateTime.now()
        );
    }
}