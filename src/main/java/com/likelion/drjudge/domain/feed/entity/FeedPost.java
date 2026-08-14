package com.likelion.drjudge.domain.feed.entity;

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

    // ⚠️ judgment 도메인 엔티티가 아직 없어서 FK 관계 대신 Long으로만 참조 (도메인 간 결합 방지).
    @Column(name = "judgment_id", nullable = false)
    private Long judgmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}