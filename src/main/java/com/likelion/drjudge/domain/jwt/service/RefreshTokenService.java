package com.likelion.drjudge.domain.jwt.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final RedisTemplate<String, String> redisTemplate;

    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, refreshToken, ttl);
    }

    public boolean isValid(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return refreshToken.equals(stored);
    }

    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
