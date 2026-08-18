package com.likelion.drjudge.domain.user.service;

import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.domain.auth.kakao.OnboardingTokenProvider;
import com.likelion.drjudge.domain.category.entity.Category;
import com.likelion.drjudge.domain.category.repository.CategoryRepository;
import com.likelion.drjudge.domain.user.dto.request.OnboardingRequest;
import com.likelion.drjudge.domain.user.dto.request.OnboardingUpdateRequest;
import com.likelion.drjudge.domain.user.dto.response.OnboardingResponse;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.exception.UserErrorCode;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.likelion.drjudge.domain.category.exception.CategoryErrorCode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final OnboardingTokenProvider onboardingTokenProvider;

    @Transactional
    public OnboardingResponse saveOnboarding(OnboardingRequest request) {
        Long userId = onboardingTokenProvider.validateAndGetUserId(request.onboardingToken());
        if (userId == null) {
            throw new BusinessException(AuthErrorCode.INVALID_ONBOARDING_TOKEN);
        }

        User user = getUser(userId);
        Set<Category> categories = resolveCategories(request.interestCategoryCodes());

        user.completeOnboarding(categories, request.ageGroup(), request.gender());

        return OnboardingResponse.from(user);
    }

    @Transactional
    public OnboardingResponse updateOnboarding(Long userId, OnboardingUpdateRequest request) {
        User user = getUser(userId);

        if (request.interestCategoryCodes() != null) {
            user.updateInterestCategories(resolveCategories(request.interestCategoryCodes()));
        }
        if (request.ageGroup() != null) {
            user.updateAgeGroup(request.ageGroup());
        }
        if (request.gender() != null) {
            user.updateGender(request.gender());
        }

        return OnboardingResponse.from(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }

    private Set<Category> resolveCategories(List<String> codes) {
        Set<Category> categories = new HashSet<>(categoryRepository.findByCodeIn(codes));
        if (categories.size() != new HashSet<>(codes).size()) {
            throw new BusinessException(CategoryErrorCode.INVALID_CATEGORY, codes);
        }
        return categories;
    }
}