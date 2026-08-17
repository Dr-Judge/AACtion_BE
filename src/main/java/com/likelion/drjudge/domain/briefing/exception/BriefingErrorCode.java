package com.likelion.drjudge.domain.briefing.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BriefingErrorCode implements ErrorCode {

    DAILY_BRIEFING_NOT_FOUND("BRIEFING_001", "존재하지 않는 브리핑입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}