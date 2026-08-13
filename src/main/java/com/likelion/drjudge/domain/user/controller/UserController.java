package com.likelion.drjudge.domain.user.controller;

import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.domain.user.dto.request.NicknameUpdateRequest;
import com.likelion.drjudge.domain.user.dto.response.UserResponse;
import com.likelion.drjudge.domain.user.service.UserService;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMyPage(Authentication authentication) {
        Long userId = currentUserId(authentication);
        UserResponse response = userService.getMyPage(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<UserResponse>> updateNickname(
            Authentication authentication,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        Long userId = currentUserId(authentication);
        UserResponse response = userService.updateNickname(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // TODO: B1의 인증(JWT) 구현이 머지되면 실제 Principal 타입으로 교체 — OnboardingController와 동일한 임시 처리.
    private Long currentUserId(Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getName())) {
            return 1L; // 로컬 테스트 전용
        }
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}