package kz.aitu.hrms.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock private StringRedisTemplate redis;
    @Mock private ValueOperations<String, String> ops;

    @InjectMocks private IdempotencyService service;

    @Test
    void alreadyProcessed_firstCall_returnsFalse() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        assertThat(service.alreadyProcessed("key1")).isFalse();
    }

    @Test
    void alreadyProcessed_duplicate_returnsTrue() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertThat(service.alreadyProcessed("key1")).isTrue();
    }
}
