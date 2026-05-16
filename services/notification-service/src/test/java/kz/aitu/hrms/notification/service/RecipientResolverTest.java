package kz.aitu.hrms.notification.service;

import feign.FeignException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kz.aitu.hrms.notification.client.EmployeeClient;
import kz.aitu.hrms.notification.client.UserClient;
import kz.aitu.hrms.notification.client.dto.UserBriefDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipientResolverTest {

    @Mock private UserClient userClient;
    @Mock private EmployeeClient employeeClient;
    @Spy  private MeterRegistry metrics = new SimpleMeterRegistry();

    @InjectMocks
    private RecipientResolver resolver;

    @Test
    void resolveUserIds_happyPath() {
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userClient.findByEmployeeId(empId)).thenReturn(new UserBriefDto(userId, "e@test.kz", empId));
        List<UUID> ids = resolver.resolveUserIds(empId);
        assertThat(ids).containsExactly(userId);
    }

    @Test
    void resolveUserIds_notFound_returnsEmpty() {
        UUID empId = UUID.randomUUID();
        when(userClient.findByEmployeeId(empId)).thenThrow(FeignException.NotFound.class);
        assertThat(resolver.resolveUserIds(empId)).isEmpty();
    }

    @Test
    void resolveUserIds_serviceUnavailable_returnsEmpty_andIncrementsMetric() {
        UUID empId = UUID.randomUUID();
        when(userClient.findByEmployeeId(empId)).thenThrow(new RuntimeException("connection refused"));
        assertThat(resolver.resolveUserIds(empId)).isEmpty();
        assertThat(metrics.counter("notification.recipient.unresolved", "method", "byEmployee").count()).isEqualTo(1.0);
    }

    @Test
    void resolveUserIds_nullEmployeeId_returnsEmpty() {
        assertThat(resolver.resolveUserIds(null)).isEmpty();
        verifyNoInteractions(userClient);
    }

    @Test
    void resolveUserIdsByPermission_serviceUnavailable_returnsEmpty() {
        when(userClient.findUserIdsByPermission("PAYROLL_VIEW")).thenThrow(new RuntimeException("down"));
        assertThat(resolver.resolveUserIdsByPermission("PAYROLL_VIEW")).isEmpty();
    }
}
