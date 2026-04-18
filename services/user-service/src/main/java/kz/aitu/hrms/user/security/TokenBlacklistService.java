package kz.aitu.hrms.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redis;

    public void blacklist(String token, long ttlMs) {
        if (ttlMs <= 0) return;
        try {
            redis.opsForValue().set(PREFIX + token, "1", Duration.ofMillis(ttlMs));
        } catch (Exception ex) {
            log.warn("Redis unavailable, blacklist skipped: {}", ex.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(PREFIX + token));
        } catch (Exception ex) {
            log.warn("Redis unavailable, blacklist check skipped: {}", ex.getMessage());
            return false;
        }
    }
}