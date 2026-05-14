package kz.aitu.hrms.notification.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.dto.NotificationDto;
import kz.aitu.hrms.notification.dto.mapper.NotificationMapper;
import kz.aitu.hrms.notification.repository.NotificationRepository;
import kz.aitu.hrms.notification.service.email.EmailRequest;
import kz.aitu.hrms.notification.service.email.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository repo;
    private final NotificationMapper mapper;
    private final IdempotencyService idempotency;
    private final EmailSender email;

    public void create(Notification n, String idempotencyKey, EmailRequest emailReq) {
        if (idempotency.alreadyProcessed(idempotencyKey)) return;
        repo.save(n);
        if (emailReq != null) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            email.send(emailReq);
                        }
                    });
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> list(UUID userId, NotificationType type,
                                      boolean onlyUnread, Pageable p) {
        Page<Notification> page;
        if (onlyUnread) {
            page = repo.findByUserIdAndReadFalseAndDeletedFalseOrderByCreatedAtDesc(userId, p);
        } else if (type != null) {
            page = repo.findByUserIdAndTypeAndDeletedFalseOrderByCreatedAtDesc(userId, type, p);
        } else {
            page = repo.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, p);
        }
        return page.map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repo.countByUserIdAndReadFalseAndDeletedFalse(userId);
    }

    public NotificationDto markRead(UUID userId, UUID id) {
        Notification n = repo.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(LocalDateTime.now());
        }
        return mapper.toDto(n);
    }

    public int markAllRead(UUID userId) {
        return repo.markAllAsRead(userId);
    }

    public void softDelete(UUID userId, UUID id) {
        Notification n = repo.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        n.setDeleted(true);
    }
}
