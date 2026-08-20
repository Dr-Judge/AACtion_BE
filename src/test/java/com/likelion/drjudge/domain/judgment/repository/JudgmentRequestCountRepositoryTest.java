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
 *
 * 예전엔 "엔티티를 로드해 필드를 바꾸고 Hibernate의 dirty checking에 맡기는" 방식으로 기존
 * 행을 증가시켰는데, 이게 CI(H2)뿐 아니라 실제 운영(MySQL)에서도
 * "Unexpected row count (expected row count 1 but was 0)"로 반복 실패했다 — Persistable
 * 구현, isNew() 판단, save() 호출 여부와 전부 무관하게 재현됐다(정확한 원인은 못 좁혔다).
 * 그래서 dirty checking에 기대는 방식 자체를 없애고, 신규 행은 EntityManager.persist()로,
 * 기존 행 증가/감소는 JudgmentRequestCountRepository의 명시적 UPDATE 쿼리
 * (incrementCount/decrementCount)로 처리한다.
 */
@DataJpaTest
class JudgmentRequestCountRepositoryTest {

    @Autowired
    private JudgmentRequestCountRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 새로_만든_카운트를_persist하면_INSERT되고_예외가_안_난다() {
        JudgmentRequestCount count = JudgmentRequestCount.create(1L, LocalDate.now());
        count.increment();

        entityManager.persist(count);
        entityManager.flush();
        entityManager.clear();

        Optional<JudgmentRequestCount> found =
                repository.findByUserIdAndRequestDate(1L, LocalDate.now());
        assertEquals(1, found.orElseThrow().getRequestCount());
    }

    @Test
    void 기존_행을_incrementCount로_증가시키면_1행_반영되고_저장된다() {
        JudgmentRequestCount count = JudgmentRequestCount.create(2L, LocalDate.now());
        entityManager.persist(count);
        entityManager.flush();
        entityManager.clear();

        int updated = repository.incrementCount(2L, LocalDate.now());
        entityManager.clear();

        assertEquals(1, updated);
        Optional<JudgmentRequestCount> found =
                repository.findByUserIdAndRequestDate(2L, LocalDate.now());
        assertEquals(1, found.orElseThrow().getRequestCount());
    }

    @Test
    void 존재하지_않는_행에_incrementCount를_호출하면_0행_반영된다() {
        int updated = repository.incrementCount(999L, LocalDate.now());

        assertEquals(0, updated);
    }

    @Test
    void decrementCount는_0_밑으로_내려가지_않는다() {
        JudgmentRequestCount count = JudgmentRequestCount.create(3L, LocalDate.now());
        entityManager.persist(count);
        entityManager.flush();
        entityManager.clear();

        int updated = repository.decrementCount(3L, LocalDate.now());
        entityManager.clear();

        assertEquals(0, updated); // requestCount가 이미 0이라 WHERE requestCount > 0에 안 걸림
        Optional<JudgmentRequestCount> found =
                repository.findByUserIdAndRequestDate(3L, LocalDate.now());
        assertEquals(0, found.orElseThrow().getRequestCount());
    }
}
