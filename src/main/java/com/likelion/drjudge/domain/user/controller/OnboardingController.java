package com.likelion.drjudge.domain.user.controller;

import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.domain.user.dto.request.OnboardingRequest;
import com.likelion.drjudge.domain.user.dto.request.OnboardingUpdateRequest;
import com.likelion.drjudge.domain.user.dto.response.OnboardingResponse;
import com.likelion.drjudge.domain.user.service.OnboardingService;
import com.likelion.drjudge.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // 로그인 전 온보딩 최초 등록
    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> saveOnboarding(
            @Valid @RequestBody OnboardingRequest request
    ) {
        OnboardingResponse response = onboardingService.saveOnboarding(request.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // 로그인 이후 수정
    @PatchMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> updateOnboarding(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody OnboardingUpdateRequest request
    ) {
        OnboardingResponse response = onboardingService.updateOnboarding(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}