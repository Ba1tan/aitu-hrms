package kz.aitu.hrms.reporting.listener;

import kz.aitu.hrms.common.event.PayrollJobCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollEventListenerTest {

    @Mock private CacheManager cacheManager;
    @Mock private Cache cache;

    @InjectMocks private PayrollEventListener listener;

    @Test
    void onPayrollJobCompleted_clearsCache() {
        PayrollJobCompletedEvent event = PayrollJobCompletedEvent.builder()
                .periodId(UUID.randomUUID()).employeeCount(5).build();
        when(cacheManager.getCache("dashboard")).thenReturn(cache);

        listener.onPayrollJobCompleted(event);

        verify(cache).clear();
    }

    @Test
    void onPayrollJobCompleted_cacheNull_doesNotThrow() {
        PayrollJobCompletedEvent event = PayrollJobCompletedEvent.builder()
                .periodId(UUID.randomUUID()).build();
        when(cacheManager.getCache("dashboard")).thenReturn(null);

        listener.onPayrollJobCompleted(event);

        verify(cache, never()).clear();
    }

    @Test
    void onPayrollJobCompleted_cacheThrows_doesNotPropagate() {
        PayrollJobCompletedEvent event = PayrollJobCompletedEvent.builder()
                .periodId(UUID.randomUUID()).build();
        when(cacheManager.getCache("dashboard")).thenReturn(cache);
        doThrow(new RuntimeException("redis down")).when(cache).clear();

        listener.onPayrollJobCompleted(event);

        // no exception propagated
    }
}
