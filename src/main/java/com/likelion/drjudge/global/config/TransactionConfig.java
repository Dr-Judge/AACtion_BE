package com.likelion.drjudge.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 외부 API 호출(OCR, 유튜브 등) 앞뒤로 DB 트랜잭션만 짧게 묶을 때 쓴다 — 같은 빈 안에서
 * @Transactional 메서드를 self-invocation하면 프록시가 안 걸려서(JudgmentService의 기존
 * @Async 관련 주석과 동일한 이유) 프로그래밍 방식 트랜잭션이 필요하다.
 */
@Configuration
public class TransactionConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
