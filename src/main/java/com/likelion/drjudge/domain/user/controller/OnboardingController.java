package com.likelion.drjudge.domain.user.controller;

import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.domain.user.dto.request.OnboardingRequest;
import com.likelion.drjudge.domain.user.dto.request.OnboardingUpdateRequest;
import com.likelion.drjudge.domain.user.dto.response.OnboardingResponse;
import com.likelion.drjudge.domain.user.service.OnboardingService;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Operation(summary = "온보딩 최초 등록", description = "회원가입 절차 중 관심 카테고리·나이대·성별을 최초로 저장한다. 로그인 전(인증 불필요) 호출되며, userId를 요청 본문으로 직접 받는다.")
    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> saveOnboarding(
            @Valid @RequestBody OnboardingRequest request
    ) {
        OnboardingResponse response = onboardingService.saveOnboarding(request.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "온보딩 정보 수정", description = "로그인 이후 관심 카테고리·나이대·성별 중 전달된 필드만 부분 수정한다.")
    @PatchMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> updateOnboarding(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody OnboardingUpdateRequest request
    ) {
        OnboardingResponse response = onboardingService.updateOnboarding(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}