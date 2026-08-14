package com.likelion.drjudge.domain.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}