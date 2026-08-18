package com.likelion.drjudge.global.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /actuator/health가 SecurityConfig에서 permitAll인데도 401을 반환하는 원인 불명
 * (Spring Boot 4에서 actuator 엔드포인트가 WebMvcEndpointHandlerMapping으로 별도
 * 등록되면서 일반 requestMatchers 문자열 매칭과 안 맞는 것으로 추정, EndpointRequest는
 * Boot 4에서 actuator-autoconfigure 밖으로 빠지면서 없어짐) — 대신 일반 MVC 컨트롤러로
 * HealthEndpoint 빈을 감싸서 노출한다. DB/Redis 등 헬스 인디케이터는 그대로 집계된다.
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    @GetMapping("/api/health")
    public ResponseEntity<String> health() {
        HealthDescriptor health = healthEndpoint.health();
        boolean up = health.getStatus().equals(Status.UP);
        return ResponseEntity.status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(health.getStatus().getCode());
    }
}
