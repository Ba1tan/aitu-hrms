package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.common.event.FraudAttemptDetectedEvent;
import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.event.dto.AttendanceRecordedEvent;
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
class AttendanceEventListenerTest {

    @Mock private NotificationFactory factory;
    @Mock private NotificationService service;
    @Mock private RecipientResolver recipients;

    @InjectMocks private AttendanceEventListener listener;

    @Test
    void onAttendanceRecorded_lateStatus_createsNotification() {
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AttendanceRecordedEvent event = AttendanceRecordedEvent.builder()
                .recordId(UUID.randomUUID()).employeeId(empId)
                .workDate(LocalDate.now()).status("LATE").build();
        var built = new NotificationFactory.BuiltNotification(
                Notification.builder().userId(userId).title("T").message("M")
                        .type(NotificationType.ATTENDANCE_ALERT).build(),
                "key", null);
        when(recipients.resolveUserIds(empId)).thenReturn(List.of(userId));
        when(factory.fromAttendanceRecorded(event, userId)).thenReturn(built);

        listener.onAttendanceRecorded(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onAttendanceRecorded_notLateStatus_skipsNotification() {
        AttendanceRecordedEvent event = AttendanceRecordedEvent.builder()
                .recordId(UUID.randomUUID()).employeeId(UUID.randomUUID())
                .workDate(LocalDate.now()).status("PRESENT").build();

        listener.onAttendanceRecorded(event);

        verifyNoInteractions(service);
        verifyNoInteractions(recipients);
    }

    @Test
    void onFraudAttemptDetected_happyPath_createsNotifications() {
        UUID userId = UUID.randomUUID();
        FraudAttemptDetectedEvent event = FraudAttemptDetectedEvent.builder()
                .employeeId(UUID.randomUUID()).fraudScore(0.95).flags("FLAG1").build();
        var built = new NotificationFactory.BuiltNotification(
                Notification.builder().userId(userId).title("T").message("M")
                        .type(NotificationType.FRAUD_ALERT).build(),
                "key", null);
        when(recipients.resolveUserIdsByPermission("ATTENDANCE_MANAGE")).thenReturn(List.of(userId));
        when(factory.fromFraudAttemptDetected(event, userId)).thenReturn(built);

        listener.onFraudAttemptDetected(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onFraudAttemptDetected_serviceThrows_doesNotPropagate() {
        FraudAttemptDetectedEvent event = FraudAttemptDetectedEvent.builder()
                .employeeId(UUID.randomUUID()).fraudScore(0.5).flags("").build();
        when(recipients.resolveUserIdsByPermission(any())).thenThrow(new RuntimeException("down"));

        listener.onFraudAttemptDetected(event);

        verify(service, never()).create(any(), any(), any());
    }
}
