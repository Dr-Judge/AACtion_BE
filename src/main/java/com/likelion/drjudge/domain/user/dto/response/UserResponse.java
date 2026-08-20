package com.likelion.drjudge.domain.user.dto.response;

import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.user.entity.User;
import java.util.List;

public record UserResponse(
        String nickname,
        int pointBalance,
        String email,
        List<String> interestCategoryCodes
) {
    public static UserResponse from(User user) {
        List<String> categoryCodes = user.getInterestCategories().stream()
                .map(Category::getCode)
                .toList();

        return new UserResponse(
                user.getNickname(),
                user.getPointBalance(),
                user.getEmail(),
                categoryCodes
        );
    }
}