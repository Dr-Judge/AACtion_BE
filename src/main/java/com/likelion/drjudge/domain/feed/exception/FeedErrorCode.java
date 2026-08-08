package com.likelion.drjudge.domain.feed.exception;

import com.likelion.drjudge.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeedErrorCode implements ErrorCode {

    FEED_NOT_FOUND("FEED_001", "존재하지 않는 게시물입니다.", HttpStatus.NOT_FOUND),
    ALREADY_LIKED("FEED_002", "이미 좋아요를 누른 게시물입니다.", HttpStatus.CONFLICT),
    PRIVATE_FEED_ACCESS_DENIED("FEED_003", "비공개 처리된 카드입니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
