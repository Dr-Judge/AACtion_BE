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

    private static final String ROTATE_AND_BLACKLIST_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "    redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]) " +
                    "    if tonumber(ARGV[5]) > 0 then " +
                    "        redis.call('SET', KEYS[2], ARGV[4], 'PX', ARGV[5]) " +
                    "    end " +
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

    public boolean rotateAndBlacklist(
            Long userId,
            String oldRefreshToken,
            String newRefreshToken,
            Duration refreshTtl,
            String oldAccessJti,
            long oldAccessRemainingMs
    ) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(ROTATE_AND_BLACKLIST_SCRIPT, Long.class);

        boolean shouldBlacklist = oldAccessJti != null && oldAccessRemainingMs > 0;
       String blacklistKey = shouldBlacklist
                ? TokenBlacklistService.KEY_PREFIX + oldAccessJti
                : KEY_PREFIX + userId;

        Long result = redisTemplate.execute(
                script,
                List.of(KEY_PREFIX + userId, blacklistKey),
                oldRefreshToken,
                newRefreshToken,
                String.valueOf(refreshTtl.toMillis()),
                TokenBlacklistService.REASON_LOGOUT,
                String.valueOf(shouldBlacklist ? oldAccessRemainingMs : 0)
        );
        return result != null && result == 1L;
    }

    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}