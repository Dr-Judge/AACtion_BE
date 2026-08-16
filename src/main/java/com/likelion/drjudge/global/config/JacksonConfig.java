package com.likelion.drjudge.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot의 기본 Jackson 자동 설정이 tools.jackson(Jackson 3) 계열의
 * JsonMapper 빈만 등록하고, 기존 com.fasterxml.jackson.databind.ObjectMapper 빈은
 * 등록하지 않는다(이 버전 조합에서 확인됨). AI 응답 JSON 직렬화/역직렬화에
 * ObjectMapper를 직접 주입해 쓰는 코드가 있어서 명시적으로 빈을 만든다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
