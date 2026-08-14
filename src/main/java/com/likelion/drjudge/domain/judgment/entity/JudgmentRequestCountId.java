package com.likelion.drjudge.domain.judgment.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class JudgmentRequestCountId implements Serializable {

    private Long userId;
    private LocalDate requestDate;

    public JudgmentRequestCountId() {
    }

    public JudgmentRequestCountId(Long userId, LocalDate requestDate) {
        this.userId = userId;
        this.requestDate = requestDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JudgmentRequestCountId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(requestDate, that.requestDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, requestDate);
    }
}
