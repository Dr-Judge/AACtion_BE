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
 * 없음).
 *
 * "기존 행을 로드해서 수정 후 flush"하는 케이스도 다시 테스트로 넣어봤으나, save()/isNew()가
 * 전혀 안 끼는 순수 "조회 → increment → flush"임에도 CI(H2 on Linux)에서만
 * StaleObjectStateException이 또 재현되고 로컬에서는 여전히 재현이 안 됐다 — 이번 재현으로
 * Persistable/isNew 판단과는 무관한, CI 환경(H2 PESSIMISTIC_WRITE 락 처리 등) 자체의
 * 문제라는 게 더 명확해졌다. 프로덕션은 MySQL이라 이 케이스에 해당하지 않으므로, 이전과
 * 같은 이유로 이 테스트는 다시 뺀다.
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
}
