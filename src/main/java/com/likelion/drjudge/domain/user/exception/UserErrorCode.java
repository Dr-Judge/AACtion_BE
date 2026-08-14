package com.likelion.drjudge.domain.user.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND("USER_001", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    ALREADY_WITHDRAWN("USER_002", "이미 탈퇴한 사용자입니다.", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("USER_003", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
    LOGIN_ID_ALREADY_EXISTS("USER_004", "이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}