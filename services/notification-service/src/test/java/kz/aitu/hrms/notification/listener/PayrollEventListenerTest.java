package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.common.event.PayrollAnomalyDetectedEvent;
import kz.aitu.hrms.common.event.PayrollJobCompletedEvent;
import kz.aitu.hrms.common.event.PayrollJobStartedEvent;
import kz.aitu.hrms.common.event.PayrollPeriodApprovedEvent;
import kz.aitu.hrms.notification.client.PayrollClient;
import kz.aitu.hrms.notification.client.dto.PayslipBriefDto;
import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.service.NotificationFactory;
import kz.aitu.hrms.notification.service.NotificationService;
import kz.aitu.hrms.notification.service.RecipientResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollEventListenerTest {

    @Mock private NotificationFactory factory;
    @Mock private NotificationService service;
    @Mock private RecipientResolver recipients;
    @Mock private PayrollClient payrollClient;

    @InjectMocks private PayrollEventListener listener;

    private NotificationFactory.BuiltNotification built(UUID userId, NotificationType type) {
        return new NotificationFactory.BuiltNotification(
                Notification.builder().userId(userId).title("T").message("M").type(type).build(),
                "key", null);
    }

    @Test
    void onPayrollJobStarted_happyPath_createsNotifications() {
        UUID userId = UUID.randomUUID();
        PayrollJobStartedEvent event = PayrollJobStartedEvent.builder()
                .periodId(UUID.randomUUID()).year(2025).month(5).employeeCount(10).build();
        when(recipients.resolveUserIdsByPermission("PAYROLL_PROCESS")).thenReturn(List.of(userId));
        when(factory.fromPayrollJobStarted(event, userId)).thenReturn(built(userId, NotificationType.SYSTEM));

        listener.onPayrollJobStarted(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onPayrollJobStarted_throws_doesNotPropagate() {
        PayrollJobStartedEvent event = PayrollJobStartedEvent.builder()
                .periodId(UUID.randomUUID()).year(2025).month(5).employeeCount(10).build();
        when(recipients.resolveUserIdsByPermission(any())).thenThrow(new RuntimeException("down"));

        listener.onPayrollJobStarted(event);

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void onPayrollJobCompleted_happyPath_createsNotifications() {
        UUID userId = UUID.randomUUID();
        PayrollJobCompletedEvent event = PayrollJobCompletedEvent.builder()
                .periodId(UUID.randomUUID()).employeeCount(5)
                .totalNet(BigDecimal.valueOf(500000)).build();
        when(recipients.resolveUserIdsByPermission("PAYROLL_PROCESS")).thenReturn(List.of(userId));
        when(factory.fromPayrollJobCompleted(event, userId)).thenReturn(built(userId, NotificationType.PAYROLL_READY));

        listener.onPayrollJobCompleted(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onPayrollAnomalyDetected_happyPath_createsNotifications() {
        UUID userId = UUID.randomUUID();
        PayrollAnomalyDetectedEvent event = PayrollAnomalyDetectedEvent.builder()
                .payslipId(UUID.randomUUID()).employeeId(UUID.randomUUID())
                .anomalyScore(BigDecimal.valueOf(0.9)).flags(List.of("FLAG1")).build();
        when(recipients.resolveUserIdsByPermission("PAYROLL_APPROVE")).thenReturn(List.of(userId));
        when(factory.fromPayrollAnomalyDetected(event, userId)).thenReturn(built(userId, NotificationType.PAYROLL_ANOMALY));

        listener.onPayrollAnomalyDetected(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onPayrollPeriodApproved_happyPath_fanOut() {
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        PayrollPeriodApprovedEvent event = PayrollPeriodApprovedEvent.builder()
                .periodId(periodId).year(2025).month(5).build();
        PayslipBriefDto slip = new PayslipBriefDto(UUID.randomUUID(), empId, BigDecimal.valueOf(90000));
        var page = new PageImpl<>(List.of(slip), PageRequest.of(0, 100), 1);
        when(payrollClient.listPayslips(eq(periodId), eq(0), eq(100))).thenReturn(page);
        when(recipients.resolveUserIds(empId)).thenReturn(List.of(userId));
        when(factory.fromPayrollPeriodApproved(eq(event), eq(slip), eq(userId)))
                .thenReturn(built(userId, NotificationType.PAYSLIP_GENERATED));

        listener.onPayrollPeriodApproved(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onPayrollPeriodApproved_payrollClientThrows_doesNotPropagate() {
        PayrollPeriodApprovedEvent event = PayrollPeriodApprovedEvent.builder()
                .periodId(UUID.randomUUID()).year(2025).month(5).build();
        when(payrollClient.listPayslips(any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("payroll down"));

        listener.onPayrollPeriodApproved(event);

        verify(service, never()).create(any(), any(), any());
    }
}
