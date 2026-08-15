package com.likelion.drjudge.domain.jwt.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private static final String KEY_PREFIX = "blacklist:";
    private static final String REASON_LOGOUT = "logout";
    private static final String REASON_WITHDRAWN = "withdrawn";

    private final RedisTemplate<String, String> redisTemplate;

    public void blacklist(String jti, long remainingValidityMs) {
        put(jti, remainingValidityMs, REASON_LOGOUT);
    }

    public void blacklistWithdrawn(String jti, long remainingValidityMs) {
        put(jti, remainingValidityMs, REASON_WITHDRAWN);
    }

    private void put(String jti, long remainingValidityMs, String reason) {
        if (jti == null || remainingValidityMs <= 0) {
            return; // 이미 만료된 토큰이면 블랙리스트에 올릴 필요 없음
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, reason, Duration.ofMillis(remainingValidityMs));
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }

    public boolean isWithdrawn(String jti) {
        if (jti == null) {
            return false;
        }
        return REASON_WITHDRAWN.equals(redisTemplate.opsForValue().get(KEY_PREFIX + jti));
    }
}