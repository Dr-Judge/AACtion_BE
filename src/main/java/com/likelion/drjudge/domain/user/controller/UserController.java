package com.likelion.drjudge.domain.user.controller;

import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.domain.user.dto.request.NicknameUpdateRequest;
import com.likelion.drjudge.domain.user.dto.response.UserResponse;
import com.likelion.drjudge.domain.user.service.UserService;
import com.likelion.drjudge.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "마이페이지 기본 정보 조회", description = "닉네임, 이메일, 포인트 잔액 등 로그인한 사용자의 기본 정보를 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMyPage(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UserResponse response = userService.getMyPage(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "닉네임 변경", description = "로그인한 사용자의 닉네임을 변경한다. 2~10자 제한.")
    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<UserResponse>> updateNickname(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        UserResponse response = userService.updateNickname(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}