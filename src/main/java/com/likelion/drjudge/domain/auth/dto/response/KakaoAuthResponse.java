package com.likelion.drjudge.domain.auth.dto.response;

import com.likelion.drjudge.domain.auth.dto.model.KakaoUserDto;
import com.likelion.drjudge.domain.user.entity.User;

public record KakaoAuthResponse(
        String token,
        String refreshToken,
        KakaoUserDto user,
        boolean isNewUser,
        Long userId
) {
    public static KakaoAuthResponse needsOnboarding(Long userId) {
        return new KakaoAuthResponse(null, null, null, true, userId);
    }

    public static KakaoAuthResponse success(TokenResponse tokens, User user) {
        return new KakaoAuthResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                new KakaoUserDto(user.getId(), user.getNickname()),
                false,
                user.getId()
        );
    }
}