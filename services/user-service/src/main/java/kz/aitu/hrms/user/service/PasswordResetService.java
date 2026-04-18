package kz.aitu.hrms.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues short-lived password-reset tokens. Tokens are stored in Redis under
 * `pwd-reset:{token}` with value = userId and a TTL matching the token lifetime.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final String PREFIX = "pwd-reset:";
    private static final SecureRandom RNG = new SecureRandom();

    private final StringRedisTemplate redis;

    @Value("${app.password-reset.ttl-seconds:1800}")
    private long ttlSeconds;

    public String issueToken(UUID userId) {
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        redis.opsForValue().set(PREFIX + token, userId.toString(), Duration.ofSeconds(ttlSeconds));
        return token;
    }

    public long ttlSeconds() {
        return ttlSeconds;
    }

    public Optional<UUID> consume(String token) {
        String key = PREFIX + token;
        String userId = redis.opsForValue().get(key);
        if (userId == null) return Optional.empty();
        redis.delete(key);
        try {
            return Optional.of(UUID.fromString(userId));
        } catch (IllegalArgumentException ex) {
            log.warn("Corrupt password-reset value: {}", userId);
            return Optional.empty();
        }
    }
}