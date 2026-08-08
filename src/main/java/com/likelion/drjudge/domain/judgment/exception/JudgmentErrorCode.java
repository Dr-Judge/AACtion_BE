package com.likelion.drjudge.domain.judgment.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JudgmentErrorCode implements ErrorCode {

    JUDGMENT_NOT_FOUND("JUDGMENT_001", "존재하지 않는 판정입니다.", HttpStatus.NOT_FOUND),
    ANONYMOUS_JUDGMENT_NOT_ALLOWED("JUDGMENT_002", "비회원은 판정을 요청할 수 없습니다.", HttpStatus.UNAUTHORIZED),
    DAILY_LIMIT_EXCEEDED("JUDGMENT_003", "일일 판정 요청 한도를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),
    EXTRACTION_FAILED("JUDGMENT_004", "입력 텍스트 추출에 실패했습니다.", HttpStatus.UNPROCESSABLE_CONTENT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
