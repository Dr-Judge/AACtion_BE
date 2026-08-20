package com.likelion.drjudge.domain.judgment.repository;

import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCount;
import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCountId;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JudgmentRequestCountRepository
        extends JpaRepository<JudgmentRequestCount, JudgmentRequestCountId> {

    // 같은 유저/날짜 행에 대한 조회-후-증가(read-then-write)를 직렬화해서
    // 동시 요청이 같은 값을 읽고 둘 다 한도 통과하는 걸 막는다 (트랜잭션 안에서만 유효).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JudgmentRequestCount> findByUserIdAndRequestDate(Long userId, LocalDate requestDate);

    // 기존 행 증가는 "엔티티를 로드해 필드를 바꾸고 flush 시 Hibernate가 dirty checking으로
    // UPDATE를 내주길" 기대하는 대신, 명시적 UPDATE 쿼리로 직접 처리한다 — 운영에서
    // 그 dirty-checking UPDATE가 "Unexpected row count (expected 1 but was 0)"로 실패하는
    // 사례가 반복 재현됐다(정확한 원인 미확정, Hibernate/Spring Data 버전 조합 문제로 추정).
    // 이 방식은 Hibernate의 엔티티 상태 추적을 아예 안 거치므로 그 문제와 무관하게 동작한다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE JudgmentRequestCount c SET c.requestCount = c.requestCount + 1 "
            + "WHERE c.userId = :userId AND c.requestDate = :requestDate")
    int incrementCount(@Param("userId") Long userId, @Param("requestDate") LocalDate requestDate);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE JudgmentRequestCount c SET c.requestCount = c.requestCount - 1 "
            + "WHERE c.userId = :userId AND c.requestDate = :requestDate AND c.requestCount > 0")
    int decrementCount(@Param("userId") Long userId, @Param("requestDate") LocalDate requestDate);
}
