package com.likelion.drjudge.domain.judgment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCount;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * JudgmentRequestCount는 @Id를 자동증가가 아니라 (userId, requestDate)로 직접 할당한다.
 * Persistable을 구현하지 않으면 Spring Data JPA가 새로 만든 엔티티도 이미 존재한다고
 * 착각해 INSERT 대신 UPDATE(merge)를 시도하고, 그 행이 실제로 없으면
 * StaleObjectStateException으로 터진다 — 실제 flush까지 타야 재현/검증되는 버그라
 * Mockito 단위 테스트로는 못 잡는다. @DataJpaTest로 진짜 저장 경로를 확인한다.
 */
@DataJpaTest
class JudgmentRequestCountRepositoryTest {

    @Autowired
    private JudgmentRequestCountRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 새로_생성한_카운트를_저장하면_INSERT되고_예외가_안_난다() {
        JudgmentRequestCount count = JudgmentRequestCount.create(1L, LocalDate.now());
        count.increment();

        repository.save(count);
        entityManager.flush();
        entityManager.clear();

        Optional<JudgmentRequestCount> found =
                repository.findByUserIdAndRequestDate(1L, LocalDate.now());
        assertEquals(1, found.orElseThrow().getRequestCount());
    }

    @Test
    void 이미_저장된_카운트를_증가시키면_UPDATE된다() {
        JudgmentRequestCount count = JudgmentRequestCount.create(2L, LocalDate.now());
        repository.save(count);
        entityManager.flush();
        entityManager.clear();

        JudgmentRequestCount loaded =
                repository.findByUserIdAndRequestDate(2L, LocalDate.now()).orElseThrow();
        loaded.increment();
        repository.save(loaded);
        entityManager.flush();
        entityManager.clear();

        Optional<JudgmentRequestCount> found =
                repository.findByUserIdAndRequestDate(2L, LocalDate.now());
        assertEquals(1, found.orElseThrow().getRequestCount());
    }
}
