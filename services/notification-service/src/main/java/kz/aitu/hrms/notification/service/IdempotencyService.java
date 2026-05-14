package kz.aitu.hrms.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redis;

    @Value("${notification.idempotency.ttl-hours:24}")
    private int ttlHours;

    public boolean alreadyProcessed(String key) {
        Boolean set = redis.opsForValue().setIfAbsent(
                "notif:idem:" + key, "1", Duration.ofHours(ttlHours));
        return Boolean.FALSE.equals(set);
    }
}
