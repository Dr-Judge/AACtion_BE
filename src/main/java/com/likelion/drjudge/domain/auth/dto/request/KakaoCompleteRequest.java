package com.likelion.drjudge.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoCompleteRequest(
        @NotBlank(message = "onboardingToken은 필수입니다.")
        String onboardingToken
) {
}