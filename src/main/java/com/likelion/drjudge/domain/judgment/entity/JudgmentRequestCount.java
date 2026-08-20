package com.likelion.drjudge.domain.judgment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @Id를 자동증가가 아니라 (userId, requestDate)로 직접 할당한다. 신규 행은
 * {@link com.likelion.drjudge.domain.judgment.service.JudgmentService}가
 * EntityManager.persist()로 직접 영속화하고, 기존 행의 증가/감소는
 * {@link com.likelion.drjudge.domain.judgment.repository.JudgmentRequestCountRepository}의
 * 명시적 UPDATE 쿼리로 처리한다 — 이 엔티티를 로드해서 필드를 바꾸고 Hibernate의 dirty
 * checking에 맡기는 방식은 운영에서 "Unexpected row count" 오류로 반복 실패해 쓰지 않는다.
 */
@Entity
@Table(name = "judgment_request_counts")
@IdClass(JudgmentRequestCountId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgmentRequestCount {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    public static JudgmentRequestCount create(Long userId, LocalDate requestDate) {
        JudgmentRequestCount count = new JudgmentRequestCount();
        count.userId = userId;
        count.requestDate = requestDate;
        count.requestCount = 0;
        return count;
    }

    public void increment() {
        this.requestCount++;
    }
}
