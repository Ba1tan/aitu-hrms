package kz.aitu.hrms.notification.repository;

import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(UUID userId, Pageable p);

    Page<Notification> findByUserIdAndTypeAndDeletedFalseOrderByCreatedAtDesc(
            UUID userId, NotificationType type, Pageable p);

    Page<Notification> findByUserIdAndReadFalseAndDeletedFalseOrderByCreatedAtDesc(
            UUID userId, Pageable p);

    long countByUserIdAndReadFalseAndDeletedFalse(UUID userId);

    @Modifying
    @Query("""
        UPDATE Notification n
           SET n.read = true, n.readAt = CURRENT_TIMESTAMP
         WHERE n.userId = :userId AND n.read = false AND n.deleted = false
    """)
    int markAllAsRead(@Param("userId") UUID userId);

    Optional<Notification> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);
}
