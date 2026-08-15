package com.likelion.drjudge.domain.feed.dto.response;

import com.likelion.drjudge.domain.feed.entity.FeedPost;
import java.time.LocalDateTime;

public record FeedPostResponse(
        Long id,
        Long judgmentId,
        int likeCount,
        LocalDateTime createdAt
) {
    public static FeedPostResponse from(FeedPost feedPost) {
        return new FeedPostResponse(
                feedPost.getId(),
                feedPost.getJudgment().getId(),
                feedPost.getLikeCount(),
                feedPost.getCreatedAt()
        );
    }
}