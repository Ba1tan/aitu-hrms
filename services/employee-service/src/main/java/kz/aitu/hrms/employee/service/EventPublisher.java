package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.common.event.EmployeeTerminatedEvent;
import kz.aitu.hrms.employee.config.RabbitConfig;
import kz.aitu.hrms.employee.event.SalaryChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(EmployeeCreatedEvent event) {
        send(RabbitConfig.RK_EMPLOYEE_CREATED, event);
    }

    public void publish(EmployeeTerminatedEvent event) {
        send(RabbitConfig.RK_EMPLOYEE_TERMINATED, event);
    }

    public void publish(SalaryChangedEvent event) {
        send(RabbitConfig.RK_SALARY_CHANGED, event);
    }

    private void send(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
        } catch (AmqpException ex) {
            log.warn("RabbitMQ unavailable, event {} dropped: {}", routingKey, ex.getMessage());
        }
    }
}