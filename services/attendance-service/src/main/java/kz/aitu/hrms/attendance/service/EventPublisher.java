package kz.aitu.hrms.attendance.service;

import kz.aitu.hrms.attendance.config.RabbitConfig;
import kz.aitu.hrms.attendance.event.AttendanceRecordedEvent;
import kz.aitu.hrms.common.event.FraudAttemptDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    public void publishAttendanceRecorded(AttendanceRecordedEvent event) {
        if (rabbitTemplate == null) {
            log.debug("RabbitTemplate unavailable; skipping attendance.recorded publish");
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.RK_ATTENDANCE_RECORDED,
                    event);
        } catch (Exception e) {
            log.warn("Failed to publish attendance.recorded: {}", e.getMessage());
        }
    }

    public void publishFraudDetected(FraudAttemptDetectedEvent event) {
        if (rabbitTemplate == null) return;
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.RK_FRAUD_DETECTED,
                    event);
        } catch (Exception e) {
            log.warn("Failed to publish attendance.fraud.detected: {}", e.getMessage());
        }
    }
}