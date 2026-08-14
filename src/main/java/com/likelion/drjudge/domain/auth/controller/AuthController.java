package com.likelion.drjudge.domain.auth.controller;

import com.likelion.drjudge.domain.auth.dto.request.LoginRequest;
import com.likelion.drjudge.domain.auth.dto.request.SignupRequest;
import com.likelion.drjudge.domain.auth.dto.response.SignupResponse;
import com.likelion.drjudge.domain.auth.dto.response.TokenResponse;
import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.domain.auth.service.AuthService;
import com.likelion.drjudge.domain.jwt.jwt.JwtTokenProvider;
import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.response.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @RequestHeader("X-Refresh-Token") String refreshToken) {
        TokenResponse tokens = authService.reissue(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestHeader("Authorization") String authorizationHeader) {

        String accessToken = authorizationHeader.replaceFirst("^Bearer ", "");
        Claims claims = jwtTokenProvider.resolveAccessClaims(accessToken);
        if (claims == null) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }

        authService.logout(principal.getId(), claims);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}