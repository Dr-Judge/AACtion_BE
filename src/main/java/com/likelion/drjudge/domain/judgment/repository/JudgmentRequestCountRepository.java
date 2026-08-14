package com.likelion.drjudge.domain.judgment.repository;

import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCount;
import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCountId;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JudgmentRequestCountRepository
        extends JpaRepository<JudgmentRequestCount, JudgmentRequestCountId> {

    Optional<JudgmentRequestCount> findByUserIdAndRequestDate(Long userId, LocalDate requestDate);
}
