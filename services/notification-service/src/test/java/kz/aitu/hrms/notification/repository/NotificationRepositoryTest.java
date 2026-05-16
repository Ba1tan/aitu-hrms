package kz.aitu.hrms.notification.repository;

import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationChannel;
import kz.aitu.hrms.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository repo;

    private Notification save(UUID userId, boolean read, boolean deleted) {
        return repo.save(Notification.builder()
                .userId(userId)
                .title("Test")
                .message("Test message")
                .type(NotificationType.INFO)
                .channel(NotificationChannel.IN_APP)
                .read(read)
                .deleted(deleted)
                .build());
    }

    @Test
    void findByUserId_sortedByCreatedAtDesc() {
        UUID userId = UUID.randomUUID();
        Notification n1 = save(userId, false, false);
        Notification n2 = save(userId, false, false);
        Page<Notification> page = repo.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getId()).isEqualTo(n2.getId());
    }

    @Test
    void markAllAsRead_returnsAffectedCount() {
        UUID userId = UUID.randomUUID();
        save(userId, false, false);
        save(userId, false, false);
        save(userId, true, false);
        int count = repo.markAllAsRead(userId);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByIdAndUserId_foreignUserReturnsEmpty() {
        UUID owner = UUID.randomUUID();
        Notification n = save(owner, false, false);
        Optional<Notification> result = repo.findByIdAndUserIdAndDeletedFalse(n.getId(), UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void deletedNotifications_excludedFromResults() {
        UUID userId = UUID.randomUUID();
        save(userId, false, true);
        Page<Notification> page = repo.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void countUnread_onlyCountsUnreadAndNotDeleted() {
        UUID userId = UUID.randomUUID();
        save(userId, false, false);
        save(userId, true, false);
        save(userId, false, true);
        long count = repo.countByUserIdAndReadFalseAndDeletedFalse(userId);
        assertThat(count).isEqualTo(1);
    }
}
