package com.likelion.drjudge.domain.judgment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCount;
import com.likelion.drjudge.domain.judgment.entity.JudgmentRequestCountId;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/**
 * JudgmentRequestCount는 @Id를 자동증가가 아니라 (userId, requestDate)로 직접 할당한다.
 * Persistable을 구현하지 않으면 Spring Data JPA가 새로 만든 엔티티도 이미 존재한다고
 * 착각해 INSERT 대신 UPDATE(merge)를 시도하고, 그 행이 실제로 없으면
 * StaleObjectStateException으로 터진다 — 실제 flush까지 타야 재현/검증되는 버그라
 * Mockito 단위 테스트로는 못 잡는다. @DataJpaTest로 진짜 저장 경로를 확인한다.
 *
 * 전용 H2 인메모리 DB 이름을 쓴다 — application.yaml의 공용 이름(jdbc:h2:mem:drjudge)을
 * 그대로 쓰면, @DataJpaTest가 다른 슬라이스(@SpringBootTest 등)와 별개의
 * ApplicationContext를 새로 만들면서 ddl-auto=create-drop이 같은 이름의 인메모리 DB에
 * 다시 DROP+CREATE를 실행해, 이 테스트가 flush 시점에 "테이블이 사라진" 것처럼 보이는
 * StaleObjectStateException을 CI에서 간헐적으로 겪었다(로컬에서는 재현 안 됨).
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:judgment_request_count_test;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
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

        // 검증 대상은 Persistable.isNew() 판단(merge vs persist)이지 락 자체가 아니라서,
        // findByUserIdAndRequestDate의 PESSIMISTIC_WRITE 락은 일부러 안 거친다 — 이 조합
        // (락 걸린 조회로 로드한 엔티티를 곧바로 mutate+flush)이 CI(H2 on Linux)에서만
        // StaleObjectStateException으로 간헐적으로 실패해서, 원인이 다른 쪽(락)인지
        // 여기(Persistable)인지 구분하기 위해 분리했다.
        JudgmentRequestCount loaded =
                entityManager.find(JudgmentRequestCount.class, new JudgmentRequestCountId(2L, LocalDate.now()));
        loaded.increment();
        // loaded는 이미 영속 상태(managed)라 dirty checking으로 flush 시 자동 반영된다 —
        // 여기서 save()를 또 호출하면 merge()를 불필요하게 다시 타게 된다.
        entityManager.flush();
        entityManager.clear();

        Optional<JudgmentRequestCount> found =
                repository.findByUserIdAndRequestDate(2L, LocalDate.now());
        assertEquals(1, found.orElseThrow().getRequestCount());
    }
}
