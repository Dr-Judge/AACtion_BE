package com.likelion.drjudge.domain.feed.dto.response;

import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.feed.entity.FeedPost;
import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.user.entity.User;
import java.time.LocalDateTime;

public record FeedPostResponse(
        Long postId,
        AuthorResponse author,
        String category,
        String title,
        String summary,
        String trustLevelLabel,
        int likeCount,
        boolean liked,
        LocalDateTime createdAt
) {
    public record AuthorResponse(Long userId, String nickname) {
    }

    public static FeedPostResponse from(FeedPost feedPost, boolean liked) {
        Judgment judgment = feedPost.getJudgment();
        User author = feedPost.getUser();
        Category category = judgment.getCategory();

        return new FeedPostResponse(
                feedPost.getId(),
                new AuthorResponse(author.getId(), author.getNickname()),
                category != null ? category.getCode() : null,
                judgment.getTitle(),
                judgment.getEvidenceSummary(),
                judgment.getTrustLevel() != null ? judgment.getTrustLevel().getLabel() : null,
                feedPost.getLikeCount(),
                liked,
                feedPost.getCreatedAt()
        );
    }
}
