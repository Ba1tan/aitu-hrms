package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.common.event.LeaveApprovedEvent;
import kz.aitu.hrms.notification.service.NotificationFactory;
import kz.aitu.hrms.notification.service.NotificationService;
import kz.aitu.hrms.notification.service.RecipientResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveEventListenerTest {

    @Mock private NotificationFactory factory;
    @Mock private NotificationService service;
    @Mock private RecipientResolver recipients;

    @InjectMocks
    private LeaveEventListener listener;

    @Test
    void onLeaveApproved_serviceThrows_doesNotPropagateException() {
        LeaveApprovedEvent event = LeaveApprovedEvent.builder()
                .requestId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .build();
        when(recipients.resolveUserIds(any())).thenThrow(new RuntimeException("connection error"));

        // Must not throw — listener swallows all exceptions
        listener.onLeaveApproved(event);

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void onLeaveApproved_happyPath_createsNotifications() {
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LeaveApprovedEvent event = LeaveApprovedEvent.builder()
                .requestId(UUID.randomUUID())
                .employeeId(empId)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .build();
        when(recipients.resolveUserIds(empId)).thenReturn(List.of(userId));
        var built = new NotificationFactory.BuiltNotification(
                kz.aitu.hrms.notification.domain.Notification.builder()
                        .userId(userId).title("T").message("M")
                        .type(kz.aitu.hrms.notification.domain.NotificationType.LEAVE_APPROVED)
                        .build(),
                "key", null);
        when(factory.fromLeaveApproved(event, userId)).thenReturn(built);

        listener.onLeaveApproved(event);

        verify(service).create(built.notification(), built.idempotencyKey(), null);
    }
}
