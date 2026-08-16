package com.likelion.drjudge.domain.feed.entity;

import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judgment_id", nullable = false)
    private Judgment judgment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static FeedPost create(Judgment judgment, User user) {
        FeedPost feedPost = new FeedPost();
        feedPost.judgment = judgment;
        feedPost.user = user;
        feedPost.isPublic = true;
        feedPost.likeCount = 0;
        return feedPost;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}