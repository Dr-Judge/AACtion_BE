package com.likelion.drjudge.domain.judgment.entity;

import com.likelion.drjudge.domain.archive.entity.ArchiveItem;
import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.global.constant.TrustLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "judgments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Judgment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 일일 한도를 예약(차감)할 때 실제로 쓴 날짜. created_at(INSERT 시각)과 다를 수 있다
    // (추출이 자정을 걸치는 경우) — 실패 시 환불은 반드시 이 날짜 기준이어야 한다.
    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 10)
    private InputType inputType;

    @Column(name = "input_text", columnDefinition = "TEXT")
    private String inputText;

    // 판정 완료 시 AI가 생성하는 의문문 형태 요약 제목 (판정 이력·공유 카드용).
    // 처리 중/실패 상태거나 옛날 데이터면 null일 수 있다.
    @Column(name = "title", length = 255)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", length = 30)
    private TrustLevel trustLevel;

    @Column(name = "conflict_detected", nullable = false)
    private boolean conflictDetected;

    @Column(name = "conflict_type", length = 30)
    private String conflictType;

    @Column(name = "conflict_description", columnDefinition = "TEXT")
    private String conflictDescription;

    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    @Column(name = "safety_notice", columnDefinition = "TEXT")
    private String safetyNotice;

    @Column(name = "guide_card_json", columnDefinition = "json")
    private String guideCardJson;

    @Column(name = "sources_json", columnDefinition = "json")
    private String sourcesJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_item_id")
    private ArchiveItem archiveItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JudgmentStatus status;

    @Column(name = "failure_error_code", length = 30)
    private String failureErrorCode;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Judgment create(User user, InputType inputType, String inputText, Category category, LocalDate requestDate) {
        Judgment judgment = new Judgment();
        judgment.user = user;
        judgment.inputType = inputType;
        judgment.inputText = inputText;
        judgment.category = category;
        judgment.requestDate = requestDate;
        judgment.status = JudgmentStatus.PROCESSING;
        judgment.conflictDetected = false;
        return judgment;
    }

    public void complete(
            String title,
            TrustLevel trustLevel,
            String evidenceSummary,
            boolean conflictDetected,
            String conflictType,
            String conflictDescription,
            String safetyNotice,
            String sourcesJson,
            String guideCardJson,
            ArchiveItem archiveItem
    ) {
        this.status = JudgmentStatus.COMPLETED;
        this.title = title;
        this.trustLevel = trustLevel;
        this.evidenceSummary = evidenceSummary;
        this.conflictDetected = conflictDetected;
        this.conflictType = conflictType;
        this.conflictDescription = conflictDescription;
        this.safetyNotice = safetyNotice;
        this.sourcesJson = sourcesJson;
        this.guideCardJson = guideCardJson;
        this.archiveItem = archiveItem;
    }

    public void fail(String failureErrorCode) {
        this.status = JudgmentStatus.FAILED;
        this.failureErrorCode = failureErrorCode;
    }
}
