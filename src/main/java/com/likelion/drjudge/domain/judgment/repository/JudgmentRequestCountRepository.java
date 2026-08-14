package com.likelion.drjudge.domain.judgment.repository;

import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCount;
import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCountId;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface JudgmentRequestCountRepository
        extends JpaRepository<JudgmentRequestCount, JudgmentRequestCountId> {

    // 같은 유저/날짜 행에 대한 조회-후-증가(read-then-write)를 직렬화해서
    // 동시 요청이 같은 값을 읽고 둘 다 한도 통과하는 걸 막는다 (트랜잭션 안에서만 유효).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JudgmentRequestCount> findByUserIdAndRequestDate(Long userId, LocalDate requestDate);
}
