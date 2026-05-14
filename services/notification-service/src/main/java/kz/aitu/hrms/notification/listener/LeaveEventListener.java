package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.common.event.LeaveApprovedEvent;
import kz.aitu.hrms.notification.config.RabbitConfig;
import kz.aitu.hrms.notification.event.dto.LeaveRejectedEvent;
import kz.aitu.hrms.notification.event.dto.LeaveRequestCreatedEvent;
import kz.aitu.hrms.notification.service.NotificationFactory;
import kz.aitu.hrms.notification.service.NotificationService;
import kz.aitu.hrms.notification.service.RecipientResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveEventListener {

    private final NotificationFactory factory;
    private final NotificationService service;
    private final RecipientResolver recipients;

    @RabbitListener(queues = RabbitConfig.Q_LEAVE_REQUEST_CREATED)
    public void onLeaveRequestCreated(LeaveRequestCreatedEvent event) {
        try {
            List<UUID> userIds;
            if (event.getManagerId() != null) {
                userIds = recipients.resolveUserIds(event.getManagerId());
            } else {
                userIds = recipients.resolveUserIdsByPermission("LEAVE_APPROVE_ALL");
            }
            for (UUID userId : userIds) {
                var built = factory.fromLeaveRequestCreated(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed LeaveRequestCreatedEvent requestId={}", event.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process LeaveRequestCreatedEvent {}: {}", event.getRequestId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_LEAVE_APPROVED)
    public void onLeaveApproved(LeaveApprovedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIds(event.getEmployeeId());
            for (UUID userId : userIds) {
                var built = factory.fromLeaveApproved(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed LeaveApprovedEvent requestId={}", event.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process LeaveApprovedEvent {}: {}", event.getRequestId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_LEAVE_REJECTED)
    public void onLeaveRejected(LeaveRejectedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIds(event.getEmployeeId());
            for (UUID userId : userIds) {
                var built = factory.fromLeaveRejected(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed LeaveRejectedEvent requestId={}", event.getRequestId());
        } catch (Exception e) {
            log.error("Failed to process LeaveRejectedEvent {}: {}", event.getRequestId(), e.getMessage(), e);
        }
    }
}
