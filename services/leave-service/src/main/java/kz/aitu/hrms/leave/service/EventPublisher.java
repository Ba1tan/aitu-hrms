package kz.aitu.hrms.leave.service;

import kz.aitu.hrms.common.event.LeaveApprovedEvent;
import kz.aitu.hrms.leave.config.RabbitConfig;
import kz.aitu.hrms.leave.event.LeaveRejectedEvent;
import kz.aitu.hrms.leave.event.LeaveRequestCreatedEvent;
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

    public void publishRequestCreated(LeaveRequestCreatedEvent event) {
        send(RabbitConfig.RK_LEAVE_REQUEST_CREATED, event);
    }

    public void publishApproved(LeaveApprovedEvent event) {
        send(RabbitConfig.RK_LEAVE_APPROVED, event);
    }

    public void publishRejected(LeaveRejectedEvent event) {
        send(RabbitConfig.RK_LEAVE_REJECTED, event);
    }

    private void send(String routingKey, Object payload) {
        if (rabbitTemplate == null) {
            log.debug("RabbitTemplate unavailable; skipping publish of {}", routingKey);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, payload);
        } catch (Exception e) {
            log.warn("Failed to publish {}: {}", routingKey, e.getMessage());
        }
    }
}