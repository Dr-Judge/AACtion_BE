package com.likelion.drjudge.domain.jwt.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private static final String KEY_PREFIX = "blacklist:";

    private final RedisTemplate<String, String> redisTemplate;
    /**
     * 로그아웃된 access token을 남은 만료시간만큼만 블랙리스트로
     * TTL을 남은 만료시간으로 맞춰서 토큰이 만료되는 시점에 Redis에서도 자동으로 같이 사라지게
     */
    public void blacklist(String jti, long remainingValidityMs) {
        if (jti == null || remainingValidityMs <= 0) {
            return; // 이미 만료된 토큰이면 블랙리스트에 올릴 필요 없음
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "logout", Duration.ofMillis(remainingValidityMs));
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}