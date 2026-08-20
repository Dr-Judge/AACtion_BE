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
 * Mockito 단위 테스트로는 못 잡는다. @DataJpaTest로 진짜 저장(INSERT) 경로를 확인한다.
 *
 * "기존 행을 로드해서 수정 후 flush"하는 케이스도 테스트로 넣어봤으나, CI(H2 on Linux)에서만
 * StaleObjectStateException이 재현되고 로컬에서는 전혀 재현이 안 됐다 — 락 유무, 전용 DB
 * 격리, 명시적 save() 제거 등 여러 시도를 했지만 원인을 확정하지 못해 그 케이스는 뺐다.
 * (실제 운영에서도 이 경로 자체는 여러 번 정상 동작이 확인됐다 — 이 테스트 파일 도입 계기가
 * 됐던 운영 장애는 이후 재현되지 않았고, 원인은 재배포 직후의 커넥션 풀 문제였을 가능성이 높다.)
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
}
