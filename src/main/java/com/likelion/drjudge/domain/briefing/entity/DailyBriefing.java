package com.likelion.drjudge.domain.briefing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_briefings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyBriefing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "briefing_date", nullable = false)
    private LocalDate briefingDate;

    @Column(name = "archive_item_id", nullable = false)
    private Long archiveItemId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static DailyBriefing create(LocalDate briefingDate, Long archiveItemId, Long categoryId) {
        DailyBriefing briefing = new DailyBriefing();
        briefing.briefingDate = briefingDate;
        briefing.archiveItemId = archiveItemId;
        briefing.categoryId = categoryId;
        return briefing;
    }
}