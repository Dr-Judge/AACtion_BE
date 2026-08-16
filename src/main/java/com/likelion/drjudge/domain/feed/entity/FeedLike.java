package com.likelion.drjudge.domain.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike {

    @EmbeddedId
    private FeedLikeId id;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static FeedLike create(Long feedPostId, Long userId) {
        FeedLike feedLike = new FeedLike();
        feedLike.id = new FeedLikeId(feedPostId, userId);
        return feedLike;
    }
}