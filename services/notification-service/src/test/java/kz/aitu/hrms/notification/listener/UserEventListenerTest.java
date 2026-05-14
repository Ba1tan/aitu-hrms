package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.event.dto.PasswordResetRequestedEvent;
import kz.aitu.hrms.notification.event.dto.UserAccountCreatedEvent;
import kz.aitu.hrms.notification.service.NotificationFactory;
import kz.aitu.hrms.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock private NotificationFactory factory;
    @Mock private NotificationService service;

    @InjectMocks private UserEventListener listener;

    @Test
    void onUserAccountCreated_happyPath_createsNotification() {
        UUID userId = UUID.randomUUID();
        UserAccountCreatedEvent event = UserAccountCreatedEvent.builder()
                .userId(userId).email("u@test.kz").firstName("Ivan").lastName("Ivanov")
                .role("EMPLOYEE").temporaryPassword("Temp123").createdAt(LocalDateTime.now()).build();
        var built = new NotificationFactory.BuiltNotification(
                Notification.builder().userId(userId).title("T").message("M")
                        .type(NotificationType.SYSTEM).build(),
                "key", null);
        when(factory.fromUserAccountCreated(event, userId)).thenReturn(built);

        listener.onUserAccountCreated(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onUserAccountCreated_serviceThrows_doesNotPropagate() {
        UUID userId = UUID.randomUUID();
        UserAccountCreatedEvent event = UserAccountCreatedEvent.builder()
                .userId(userId).email("u@test.kz").firstName("Ivan").lastName("Ivanov")
                .role("EMPLOYEE").temporaryPassword("Temp123").createdAt(LocalDateTime.now()).build();
        when(factory.fromUserAccountCreated(any(), any())).thenThrow(new RuntimeException("down"));

        listener.onUserAccountCreated(event);

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void onPasswordResetRequested_happyPath_createsNotification() {
        UUID userId = UUID.randomUUID();
        PasswordResetRequestedEvent event = PasswordResetRequestedEvent.builder()
                .userId(userId).email("u@test.kz").firstName("Ivan")
                .resetToken("token123").ttlSeconds(3600).build();
        var built = new NotificationFactory.BuiltNotification(
                Notification.builder().userId(userId).title("T").message("M")
                        .type(NotificationType.PASSWORD_RESET).build(),
                "key", null);
        when(factory.fromPasswordResetRequested(event, userId)).thenReturn(built);

        listener.onPasswordResetRequested(event);

        verify(service).create(any(), any(), any());
    }
}
