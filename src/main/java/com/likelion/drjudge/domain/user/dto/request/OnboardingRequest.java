package com.likelion.drjudge.domain.user.dto.request;

import com.likelion.drjudge.domain.user.entity.AgeGroup;
import com.likelion.drjudge.domain.user.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OnboardingRequest(
        @NotBlank(message = "onboardingToken은 필수입니다.")
        String onboardingToken,
        @NotEmpty(message = "관심 카테고리를 선택해주세요.")
        List<String> interestCategoryCodes,
        @NotNull(message = "나이대를 선택해주세요.")
        AgeGroup ageGroup,
        @NotNull(message = "성별을 선택해주세요.")
        Gender gender
) {
}