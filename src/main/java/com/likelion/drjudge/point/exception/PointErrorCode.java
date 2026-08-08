package com.likelion.drjudge.point.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {

    ALREADY_CHECKED_IN_TODAY("POINT_001", "이미 오늘 출석체크를 완료했습니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
