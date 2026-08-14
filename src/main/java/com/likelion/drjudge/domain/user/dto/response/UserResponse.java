package com.likelion.drjudge.domain.user.dto.response;

import com.likelion.drjudge.domain.user.entity.User;

public record UserResponse(
        String nickname,
        int pointBalance
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getNickname(), user.getPointBalance());
    }
}