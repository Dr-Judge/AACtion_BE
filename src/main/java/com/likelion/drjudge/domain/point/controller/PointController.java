package com.likelion.drjudge.domain.point.controller;

import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.domain.point.dto.response.PointHistoryPageResponse;
import com.likelion.drjudge.domain.point.service.PointService;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PointHistoryPageResponse>> getPointHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = currentUserId(authentication);
        PointHistoryPageResponse response = pointService.getPointHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // TODO: B1의 인증(JWT) 구현이 머지되면 실제 Principal 타입으로 교체 — 다른 컨트롤러들과 동일한 임시 처리.
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