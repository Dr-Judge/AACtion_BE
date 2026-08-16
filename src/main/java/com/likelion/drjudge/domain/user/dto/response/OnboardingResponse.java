package com.likelion.drjudge.domain.user.dto.response;

import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.user.entity.AgeGroup;
import com.likelion.drjudge.domain.user.entity.Gender;
import com.likelion.drjudge.domain.user.entity.User;
import java.util.List;

public record OnboardingResponse(
        List<String> interestCategoryCodes,
        AgeGroup ageGroup,
        Gender gender,
        boolean onboardingCompleted
) {

    public static OnboardingResponse from(User user) {
        List<String> categoryCodes = user.getInterestCategories().stream()
                .map(Category::getCode)
                .toList();

        return new OnboardingResponse(
                categoryCodes,
                user.getAgeGroup(),
                user.getGender(),
                user.isOnboardingCompleted()
        );
    }
}