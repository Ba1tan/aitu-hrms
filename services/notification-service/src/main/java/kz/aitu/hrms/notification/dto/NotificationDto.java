package kz.aitu.hrms.notification.dto;

import kz.aitu.hrms.notification.domain.NotificationChannel;
import kz.aitu.hrms.notification.domain.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        String title,
        String message,
        NotificationType type,
        NotificationChannel channel,
        boolean isRead,
        LocalDateTime readAt,
        String referenceType,
        UUID referenceId,
        LocalDateTime createdAt
) {}
