package com.likelion.drjudge.domain.user.controller;

import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.domain.user.dto.request.NicknameUpdateRequest;
import com.likelion.drjudge.domain.user.dto.response.UserResponse;
import com.likelion.drjudge.domain.user.service.UserService;
import com.likelion.drjudge.global.response.ApiResponse;
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
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMyPage(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UserResponse response = userService.getMyPage(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<UserResponse>> updateNickname(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        UserResponse response = userService.updateNickname(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}