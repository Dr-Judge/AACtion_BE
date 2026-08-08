package com.likelion.drjudge.share.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShareErrorCode implements ErrorCode {

    SHARE_LINK_NOT_FOUND("SHARE_001", "존재하지 않거나 회수된 공유링크입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
