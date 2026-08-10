// domain/user/controller/OnboardingController.java
package com.likelion.drjudge.domain.user.controller;

import com.likelion.drjudge.domain.user.dto.request.OnboardingRequest;
import com.likelion.drjudge.domain.user.dto.response.OnboardingResponse;
import com.likelion.drjudge.domain.user.service.OnboardingService;
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
            @RequestBody OnboardingRequest request
    ) {
        Long userId = currentUserId(authentication);
        OnboardingResponse response = onboardingService.updateOnboarding(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // TODO: B1의 인증(JWT) 구현이 머지되면 실제 Principal 타입으로 교체.
    // 현재는 JWT subject(sub) 클레임에 userId 문자열이 들어있다고 가정하고 임시로 파싱만 해둔 상태.
    private Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}