package com.likelion.drjudge.domain.auth.dto.response;

import com.likelion.drjudge.domain.auth.dto.model.KakaoUserDto;
import com.likelion.drjudge.domain.user.entity.User;

public record KakaoAuthResponse(
        String token,
        String refreshToken,
        KakaoUserDto user,
        boolean isNewUser,
        String onboardingToken
) {
    public static KakaoAuthResponse needsOnboarding(Long userId, String onboardingToken) {
        return new KakaoAuthResponse(null, null, null, true, onboardingToken);
    }

    public static KakaoAuthResponse success(TokenResponse tokens, User user) {
        return new KakaoAuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                new KakaoUserDto(user.getNickname()),
                false,
                null
        );
    }
}