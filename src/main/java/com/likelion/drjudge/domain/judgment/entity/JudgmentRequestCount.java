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
