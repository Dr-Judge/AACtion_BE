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
 * @Id를 자동증가가 아니라 (userId, requestDate)로 직접 할당한다. 예전엔 Persistable을 구현해
 * INSERT/UPDATE를 애플리케이션이 직접 구분했으나, 운영에서 신규 행 첫 요청마다
 * StaleObjectStateException(merge가 UPDATE 0행)이 재현되는 문제가 있었다 — 원인 불문하고
 * "새 엔티티 vs 기존 엔티티" 구분 자체를 코드에서 없애는 쪽으로 바꿨다. 이제 항상
 * {@link com.likelion.drjudge.domain.judgment.repository.JudgmentRequestCountRepository#ensureRowExists}
 * (INSERT ... ON DUPLICATE KEY UPDATE)로 행 존재를 먼저 보장한 뒤, 조회해서 얻은(이미 영속 상태인)
 * 엔티티만 증가시킨다 — save()를 호출할 일 자체가 없어 isNew 판정이 필요 없다.
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

    public void decrement() {
        if (this.requestCount > 0) {
            this.requestCount--;
        }
    }
}
