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
 * 예전엔 Persistable을 구현해 새 엔티티/기존 엔티티를 repository.save()가 스스로 판단하게
 * 했으나, 운영에서 신규 행 첫 요청마다 StaleObjectStateException(merge가 UPDATE 0행)이
 * 계속 재현됐다 — Persistable.isNew()가 true를 반환하는데도 실제로는 merge()가 나가는
 * 것까지 로그로 확인했지만 정확한 원인은 못 좁혔다. 그래서 그 판단 자체를 없앴다:
 * 새 행이라고 이미 알고 있는 경우(findByUserIdAndRequestDate가 empty)엔
 * EntityManager.persist()를 직접 호출한다(JPA persist()는 항상 INSERT만 함, merge() 여지가
 * 없음). 기존 행이면 repository.save()를 아예 호출하지 않고 dirty checking에만 맡긴다 —
 * 이 두 경로 모두 "새 엔티티인지 판단"이 필요 없어서, 그 판단 로직 자체가 원인이었던
 * CI 전용 플레이키(StaleObjectStateException)도 같이 없어질 것으로 기대한다.
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
    void 기존_행을_조회해서_증가시키면_save_없이도_dirty_checking으로_UPDATE된다() {
        JudgmentRequestCount count = JudgmentRequestCount.create(2L, LocalDate.now());
        entityManager.persist(count);
        entityManager.flush();
        entityManager.clear();

        JudgmentRequestCount loaded =
                repository.findByUserIdAndRequestDate(2L, LocalDate.now()).orElseThrow();
        loaded.increment();
        entityManager.flush();
        entityManager.clear();

        Optional<JudgmentRequestCount> found =
                repository.findByUserIdAndRequestDate(2L, LocalDate.now());
        assertEquals(1, found.orElseThrow().getRequestCount());
    }
}
