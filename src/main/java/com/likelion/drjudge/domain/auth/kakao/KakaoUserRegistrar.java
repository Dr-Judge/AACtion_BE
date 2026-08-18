package com.likelion.drjudge.domain.auth.kakao;

import com.likelion.drjudge.domain.auth.dto.response.KakaoUserInfoResponse;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
        return userRepository.saveAndFlush(user);
    }
}