package kz.aitu.hrms.notification.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationChannel;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.dto.mapper.NotificationMapper;
import kz.aitu.hrms.notification.repository.NotificationRepository;
import kz.aitu.hrms.notification.service.email.EmailRequest;
import kz.aitu.hrms.notification.service.email.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository repo;
    @Mock private NotificationMapper mapper;
    @Mock private IdempotencyService idempotency;
    @Mock private EmailSender email;

    @InjectMocks
    private NotificationService service;

    private Notification buildNotification(UUID userId) {
        return Notification.builder()
                .userId(userId)
                .title("Test")
                .message("Msg")
                .type(NotificationType.INFO)
                .channel(NotificationChannel.IN_APP)
                .build();
    }

    @Test
    void create_whenIdempotencyHit_skipsSave() {
        Notification n = buildNotification(UUID.randomUUID());
        when(idempotency.alreadyProcessed("key")).thenReturn(true);
        service.create(n, "key", null);
        verify(repo, never()).save(any());
    }

    @Test
    void create_whenNew_savesNotification() {
        Notification n = buildNotification(UUID.randomUUID());
        when(idempotency.alreadyProcessed("key")).thenReturn(false);
        service.create(n, "key", null);
        verify(repo).save(n);
    }

    @Test
    void softDelete_foreignUser_throws404() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.softDelete(userId, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markRead_foreignUser_throws404() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(repo.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markRead(userId, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markRead_alreadyRead_doesNotChangeReadAt() {
        UUID userId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Notification n = buildNotification(userId);
        n.setRead(true);
        n.setReadAt(java.time.LocalDateTime.of(2025, 1, 1, 0, 0));
        when(repo.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.of(n));
        when(mapper.toDto(n)).thenReturn(null);
        service.markRead(userId, id);
        assertThat(n.getReadAt()).isEqualTo(java.time.LocalDateTime.of(2025, 1, 1, 0, 0));
    }
}
