package com.likelion.drjudge.domain.judgment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * @Id를 자동증가가 아니라 (userId, requestDate)로 직접 할당하기 때문에, Persistable을
 * 구현하지 않으면 Spring Data JPA가 새로 만든 엔티티도 "이미 DB에 있다"고 착각해서
 * INSERT 대신 UPDATE(merge)를 시도한다 — 그 유저가 그 날짜에 아직 행이 없으면 UPDATE가
 * 0행 적용되며 StaleObjectStateException이 터진다(그 유저의 그날 첫 요청마다 재현됨).
 * isNew를 명시적으로 관리해서 새로 만든 것과 DB에서 읽어온 것을 구분한다.
 */
@Entity
@Table(name = "judgment_request_counts")
@IdClass(JudgmentRequestCountId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JudgmentRequestCount implements Persistable<JudgmentRequestCountId> {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "request_count", nullable = false)
    private int requestCount;

    @Transient
    private boolean isNew = true;

    public static JudgmentRequestCount create(Long userId, LocalDate requestDate) {
        JudgmentRequestCount count = new JudgmentRequestCount();
        count.userId = userId;
        count.requestDate = requestDate;
        count.requestCount = 0;
        return count;
    }

    @Override
    public JudgmentRequestCountId getId() {
        return new JudgmentRequestCountId(userId, requestDate);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
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
