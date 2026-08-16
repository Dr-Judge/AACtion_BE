package com.likelion.drjudge.domain.jwt.service;

import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("아이디 또는 비밀번호가 일치하지 않습니다."));

        return CustomUserPrincipal.from(user);
    }

    public UserDetails loadUserByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다."));
        return CustomUserPrincipal.from(user);
    }
}