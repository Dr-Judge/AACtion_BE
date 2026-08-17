package com.likelion.drjudge.domain.share.entity;

import com.likelion.drjudge.domain.judgment.entity.Judgment;
import com.likelion.drjudge.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "share_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judgment_id", nullable = false)
    private Judgment judgment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 32)
    private String token;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ShareLink create(Judgment judgment, User user, String token) {
        ShareLink shareLink = new ShareLink();
        shareLink.judgment = judgment;
        shareLink.user = user;
        shareLink.token = token;
        shareLink.isActive = true;
        return shareLink;
    }

    public void revoke() {
        this.isActive = false;
    }
}