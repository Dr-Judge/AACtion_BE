package com.likelion.drjudge.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE("COMMON_400", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON_401", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON_403", "권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("COMMON_404", "리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PAYLOAD_TOO_LARGE("COMMON_413", "요청 본문이 너무 큽니다.", HttpStatus.PAYLOAD_TOO_LARGE),
    INTERNAL_SERVER_ERROR("COMMON_500", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
