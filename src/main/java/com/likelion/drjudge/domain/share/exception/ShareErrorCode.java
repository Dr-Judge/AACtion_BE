package com.likelion.drjudge.domain.share.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ShareErrorCode implements ErrorCode {

    SHARE_LINK_NOT_FOUND("SHARE_001", "존재하지 않거나 회수된 공유링크입니다.", HttpStatus.NOT_FOUND),
    NOT_JUDGMENT_OWNER("SHARE_002", "본인의 판정 결과만 공유할 수 있습니다.", HttpStatus.FORBIDDEN),
    JUDGMENT_NOT_COMPLETED("SHARE_003", "판정이 완료된 결과만 공유할 수 있습니다.", HttpStatus.CONFLICT),
    NO_ACTIVE_SHARE_LINK("SHARE_004", "회수할 활성 공유링크가 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}