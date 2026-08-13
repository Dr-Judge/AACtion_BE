package com.likelion.drjudge.domain.user.controller;

import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.domain.user.dto.request.OnboardingRequest;
import com.likelion.drjudge.domain.user.dto.request.OnboardingUpdateRequest;
import com.likelion.drjudge.domain.user.dto.response.OnboardingResponse;
import com.likelion.drjudge.domain.user.service.OnboardingService;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> saveOnboarding(
            Authentication authentication,
            @Valid @RequestBody OnboardingRequest request
    ) {
        Long userId = currentUserId(authentication);
        OnboardingResponse response = onboardingService.saveOnboarding(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> updateOnboarding(
            Authentication authentication,
            @Valid @RequestBody OnboardingUpdateRequest request
    ) {
        Long userId = currentUserId(authentication);
        OnboardingResponse response = onboardingService.updateOnboarding(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // TODO: B1의 인증(JWT) 구현이 머지되면 이 메서드는 실제 Principal 타입으로 전면 교체.
    // 지금은 spring-boot-starter-security만 있고 SecurityFilterChain/JWT 필터가 없어서
    // 인증 없이 호출하면 Spring Boot가 만든 기본 유저("user", 랜덤 비밀번호)로 Basic 인증이 붙는다.
    // 그 경우 authentication.getName()이 "user"라는 문자열이라 Long.valueOf(...)가 그대로
    // NumberFormatException(처리 안 하면 500)을 던지므로, 최소한 401로 내려가도록 방어해둔다.
    // ⚠️ 로컬 테스트 시 TempSecurityConfig(permitAll)를 임시로 쓰는 경우 anonymousUser 분기를 타게 되는데,
    //    그 파일은 PR 올리기 전 반드시 삭제할 것 — 이 분기까지 지우라는 뜻은 아님.
    private Long currentUserId(Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return 1L; // DB에 미리 넣어둔 테스트 유저(id=1) — 로컬 테스트 전용
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}