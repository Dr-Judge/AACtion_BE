package com.likelion.drjudge.domain.user.dto.response;

import com.likelion.drjudge.domain.user.entity.User;

public record UserResponse(
        Long id,
        String nickname,
        int pointBalance
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNickname(), user.getPointBalance());
    }
}