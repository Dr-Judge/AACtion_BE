package com.likelion.drjudge.domain.user.dto.request;

import com.likelion.drjudge.domain.user.entity.AgeGroup;
import com.likelion.drjudge.domain.user.entity.Gender;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * PATCH /users/me/onboarding 전용 요청 DTO.
 * POST용 OnboardingRequest와 달리 모든 필드가 선택(nullable)이다 — 부분 수정이므로
 * 전달 안 된 필드는 그대로 두고, 전달된 필드만 반영한다(OnboardingService 참고).
 *
 * interestCategoryCodes 는 null(= 이 필드 수정 안 함)은 허용하되,
 * 빈 배열([])이 오면 "카테고리를 0개로 만들겠다"는 잘못된 요청이므로 @Size(min = 1)로 막는다.
 */
public record OnboardingUpdateRequest(

        @Size(min = 1, message = "관심 카테고리를 수정하려면 1개 이상 선택해주세요.")
        List<String> interestCategoryCodes,

        AgeGroup ageGroup,

        Gender gender
) {
}