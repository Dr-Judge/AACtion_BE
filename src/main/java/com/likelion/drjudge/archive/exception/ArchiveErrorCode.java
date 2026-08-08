package com.likelion.drjudge.archive.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArchiveErrorCode implements ErrorCode {

    ARCHIVE_ITEM_NOT_FOUND("ARCHIVE_001", "존재하지 않는 아카이브 항목입니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
