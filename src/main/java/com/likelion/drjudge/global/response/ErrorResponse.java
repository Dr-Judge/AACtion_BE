package com.likelion.drjudge.global.response;

import com.likelion.drjudge.global.exception.ErrorCode;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String errorCode,
        String message,
        String path,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getCode(),
                message,
                path,
                LocalDateTime.now()
        );
    }
}