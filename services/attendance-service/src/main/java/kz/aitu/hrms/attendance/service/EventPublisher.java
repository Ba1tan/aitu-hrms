package kz.aitu.hrms.attendance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.attendance.config.RabbitConfig;
import kz.aitu.hrms.attendance.event.AttendanceRecordedEvent;
import kz.aitu.hrms.common.audit.AuditEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private static final String SERVICE = "attendance-service";

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    public void publishAttendanceRecorded(AttendanceRecordedEvent event) {
        send(RabbitConfig.RK_ATTENDANCE_RECORDED, event, "attendance.recorded");
    }

    /** Emit a system-wide audit row (consumed by user-service). */
    public void audit(String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        send(RabbitConfig.RK_AUDIT,
                AuditEvents.build(SERVICE, action, entityType, entityId, oldValue, newValue, objectMapper),
                "audit.recorded");
    }

    private void send(String routingKey, Object event, String label) {
        if (rabbitTemplate == null) {
            log.debug("RabbitTemplate unavailable; skipping {} publish", label);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.warn("Failed to publish {}: {}", label, e.getMessage());
        }
    }
}