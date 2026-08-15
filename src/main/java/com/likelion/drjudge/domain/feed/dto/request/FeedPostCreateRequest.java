package com.likelion.drjudge.domain.feed.dto.request;

import jakarta.validation.constraints.NotNull;

public record FeedPostCreateRequest(
        @NotNull(message = "게시할 판정 ID가 필요합니다.")
        Long judgmentId
) {
}