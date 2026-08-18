package com.likelion.drjudge.domain.briefing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "briefing_views")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BriefingView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "daily_briefing_id", nullable = false)
    private Long dailyBriefingId;

    @Column(name = "opened_at", insertable = false, updatable = false)
    private LocalDateTime openedAt;

    public static BriefingView create(Long userId, Long dailyBriefingId) {
        BriefingView view = new BriefingView();
        view.userId = userId;
        view.dailyBriefingId = dailyBriefingId;
        return view;
    }
}