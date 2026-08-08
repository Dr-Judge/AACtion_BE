package com.likelion.drjudge.global.response;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String errorCode,
        String message,
        String path,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(status, errorCode, message, path, LocalDateTime.now());
    }
}
