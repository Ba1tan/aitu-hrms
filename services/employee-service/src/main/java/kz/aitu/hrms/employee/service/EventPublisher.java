package kz.aitu.hrms.employee.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.common.audit.AuditEvents;
import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.common.event.EmployeeTerminatedEvent;
import kz.aitu.hrms.employee.config.RabbitConfig;
import kz.aitu.hrms.employee.event.SalaryChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private static final String SERVICE = "employee-service";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publish(EmployeeCreatedEvent event) {
        send(RabbitConfig.RK_EMPLOYEE_CREATED, event);
    }

    public void publish(EmployeeTerminatedEvent event) {
        send(RabbitConfig.RK_EMPLOYEE_TERMINATED, event);
    }

    public void publish(SalaryChangedEvent event) {
        send(RabbitConfig.RK_SALARY_CHANGED, event);
    }

    /** Emit a system-wide audit row (consumed by user-service). */
    public void audit(String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        send(RabbitConfig.RK_AUDIT,
                AuditEvents.build(SERVICE, action, entityType, entityId, oldValue, newValue, objectMapper));
    }

    private void send(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
        } catch (AmqpException ex) {
            log.warn("RabbitMQ unavailable, event {} dropped: {}", routingKey, ex.getMessage());
        }
    }
}