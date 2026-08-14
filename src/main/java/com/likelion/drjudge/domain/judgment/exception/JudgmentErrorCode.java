package com.likelion.drjudge.domain.judgment.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JudgmentErrorCode implements ErrorCode {

    JUDGMENT_NOT_FOUND("JUDGMENT_001", "존재하지 않는 판정입니다.", HttpStatus.NOT_FOUND),
    // JUDGMENT_002(비회원 접근 차단)는 이제 Spring Security가 컨트롤러 진입 전에
    // COMMON_401로 처리한다 — 여기서 던질 일이 없어져서 제거함.
    DAILY_LIMIT_EXCEEDED("JUDGMENT_003", "일일 판정 요청 한도를 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),
    EXTRACTION_FAILED("JUDGMENT_004", "입력 텍스트 추출에 실패했습니다.", HttpStatus.UNPROCESSABLE_CONTENT),
    AI_SERVICE_UNAVAILABLE("JUDGMENT_005", "AI 판정 서비스에 일시적으로 연결할 수 없어요. 다시 시도해주세요.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
