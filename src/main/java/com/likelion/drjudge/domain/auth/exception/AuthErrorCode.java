package com.likelion.drjudge.domain.auth.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    EXPIRED_TOKEN("AUTH_001", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("AUTH_002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    KAKAO_AUTH_FAILED("AUTH_003", "카카오 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    ALREADY_LOGGED_OUT("AUTH_004", "이미 로그아웃된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("AUTH_005", "아이디 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN("AUTH_006", "유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_ONBOARDING_TOKEN("AUTH_007", "유효하지 않거나 만료된 온보딩 토큰입니다.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}