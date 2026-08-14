package com.likelion.drjudge.domain.jwt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private static final String ROTATE_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "    redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]) " +
                    "    return 1 " +
                    "else " +
                    "    return 0 " +
                    "end";

    private final RedisTemplate<String, String> redisTemplate;

    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, refreshToken, ttl);
    }

    public boolean isValid(Long userId, String refreshToken) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        return refreshToken.equals(stored);
    }

    public boolean rotate(Long userId, String oldRefreshToken, String newRefreshToken, Duration ttl) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ROTATE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(
                script,
                List.of(KEY_PREFIX + userId),
                oldRefreshToken,
                newRefreshToken,
                String.valueOf(ttl.toMillis())
        );
        return result != null && result == 1L;
    }

    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}