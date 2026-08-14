package com.likelion.drjudge.domain.user.service;

import com.likelion.drjudge.domain.user.dto.request.NicknameUpdateRequest;
import com.likelion.drjudge.domain.user.dto.response.UserResponse;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.exception.UserErrorCode;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /** PATCH /users/me/nickname */
    @Transactional
    public UserResponse updateNickname(Long userId, NicknameUpdateRequest request) {
        User user = getUser(userId);
        user.updateNickname(request.nickname());
        return UserResponse.from(user);
    }

    public UserResponse getMyPage(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    }
}