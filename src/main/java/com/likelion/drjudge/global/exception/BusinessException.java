package com.likelion.drjudge.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object context;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.context = null;
    }

    public BusinessException(ErrorCode errorCode, Object context) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.context = context;
    }
}
