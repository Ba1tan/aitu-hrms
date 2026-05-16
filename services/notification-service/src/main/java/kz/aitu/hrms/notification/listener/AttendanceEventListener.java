package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.common.event.FraudAttemptDetectedEvent;
import kz.aitu.hrms.notification.config.RabbitConfig;
import kz.aitu.hrms.notification.event.dto.AttendanceRecordedEvent;
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
public class AttendanceEventListener {

    private final NotificationFactory factory;
    private final NotificationService service;
    private final RecipientResolver recipients;

    @RabbitListener(queues = RabbitConfig.Q_ATTENDANCE_RECORDED)
    public void onAttendanceRecorded(AttendanceRecordedEvent event) {
        try {
            if (!"LATE".equals(event.getStatus())) return;
            List<UUID> userIds = recipients.resolveUserIds(event.getEmployeeId());
            for (UUID userId : userIds) {
                var built = factory.fromAttendanceRecorded(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed AttendanceRecordedEvent recordId={} status=LATE", event.getRecordId());
        } catch (Exception e) {
            log.error("Failed to process AttendanceRecordedEvent {}: {}", event.getRecordId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_ATTENDANCE_FRAUD)
    public void onFraudAttemptDetected(FraudAttemptDetectedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIdsByPermission("ATTENDANCE_MANAGE");
            for (UUID userId : userIds) {
                var built = factory.fromFraudAttemptDetected(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed FraudAttemptDetectedEvent employeeId={}", event.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to process FraudAttemptDetectedEvent {}: {}", event.getEmployeeId(), e.getMessage(), e);
        }
    }
}
