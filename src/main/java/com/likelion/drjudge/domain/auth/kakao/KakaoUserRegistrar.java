package com.likelion.drjudge.domain.auth.kakao;

import com.likelion.drjudge.domain.auth.dto.response.KakaoUserInfoResponse;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class KakaoUserRegistrar {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User register(String kakaoId, KakaoUserInfoResponse kakaoUserInfo) {
        User user = User.createKakaoUser(kakaoId, kakaoUserInfo.resolveNickname());

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMostSpecificCause().getMessage();
            if (msg != null && msg.contains("uq_users_kakao_id")) {
                return userRepository.findByKakaoId(kakaoId)
                        .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE));
            }
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
    }
}