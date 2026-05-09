package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.common.event.PayrollAnomalyDetectedEvent;
import kz.aitu.hrms.common.event.PayrollJobCompletedEvent;
import kz.aitu.hrms.common.event.PayrollJobStartedEvent;
import kz.aitu.hrms.common.event.PayrollPeriodApprovedEvent;
import kz.aitu.hrms.payroll.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventPublisher {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    public void publishJobStarted(PayrollJobStartedEvent event) {
        send(RabbitConfig.RK_PAYROLL_JOB_STARTED, event);
    }

    public void publishJobCompleted(PayrollJobCompletedEvent event) {
        send(RabbitConfig.RK_PAYROLL_JOB_COMPLETED, event);
    }

    public void publishAnomaly(PayrollAnomalyDetectedEvent event) {
        send(RabbitConfig.RK_PAYROLL_ANOMALY, event);
    }

    public void publishPeriodApproved(PayrollPeriodApprovedEvent event) {
        send(RabbitConfig.RK_PAYROLL_PERIOD_APPROVED, event);
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