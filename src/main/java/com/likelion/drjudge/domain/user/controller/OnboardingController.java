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

    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> saveOnboarding(
            @Valid @RequestBody OnboardingRequest request
    ) {
        Long userId = currentUserId();
        OnboardingResponse response = onboardingService.saveOnboarding(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> updateOnboarding(
            @Valid @RequestBody OnboardingUpdateRequest request
    ) {
        Long userId = currentUserId();
        OnboardingResponse response = onboardingService.updateOnboarding(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // TODO: B1의 인증(JWT) 구현이 머지되면 이 메서드는 실제 Principal 타입으로 전면 교체.
    // Authentication을 메서드 파라미터로 받으면 Spring Security가 익명 요청의 principal을
    // null로 masking해버려서, SecurityContextHolder에서 직접 꺼내는 방식으로 우회한다.
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}