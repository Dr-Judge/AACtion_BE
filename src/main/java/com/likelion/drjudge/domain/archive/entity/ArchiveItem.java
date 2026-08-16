package com.likelion.drjudge.domain.archive.entity;

import com.likelion.drjudge.global.constant.TrustLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * archive_items 매핑 엔티티. 등록/수정/삭제는 관리자가 SQL로 직접 처리한다(R-TUSYUO,
 * 전용 admin API 없음) — 이 엔티티는 다른 도메인(브리핑, 판정)이 읽기 전용으로만 사용한다.
 */
@Entity
@Table(name = "archive_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchiveItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String target;

    @Column(nullable = false, length = 200)
    private String effect;

    @Column(name = "condition_scope", length = 300)
    private String conditionScope;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", nullable = false, length = 30)
    private TrustLevel trustLevel;

    @Column(name = "evidence_source_type", length = 50)
    private String evidenceSourceType;

    @Column(name = "evidence_sources_json", columnDefinition = "json")
    private String evidenceSourcesJson;

    @Column(name = "evidence_summary", nullable = false, columnDefinition = "TEXT")
    private String evidenceSummary;

    @Column(length = 30)
    private String version;

    @Column(name = "managed_by", length = 50)
    private String managedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
