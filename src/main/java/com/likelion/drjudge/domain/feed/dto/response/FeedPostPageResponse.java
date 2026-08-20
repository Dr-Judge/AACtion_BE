package com.likelion.drjudge.domain.feed.dto.response;

import java.util.List;

public record FeedPostPageResponse(
        List<FeedPostResponse> items,
        int page,
        int totalPages
) {
}
