package com.likelion.drjudge.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 판정 AI 서비스(Python) 호출용 RestClient. 타임아웃·인증 헤더는
 * docs/AI_SERVICE_CONTRACT.md 에 정의된 정책을 그대로 따른다.
 */
@Configuration
public class RestClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    @Bean
    public RestClient aiServiceRestClient(
            @Value("${app.ai-service.base-url}") String baseUrl,
            @Value("${app.ai-service.api-key}") String apiKey) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(INTERNAL_API_KEY_HEADER, apiKey)
                .requestFactory(factory)
                .build();
    }
}
