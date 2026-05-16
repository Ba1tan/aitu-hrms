package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.common.event.EmployeeTerminatedEvent;
import kz.aitu.hrms.common.event.SalaryChangedEvent;
import kz.aitu.hrms.notification.config.RabbitConfig;
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
public class EmployeeEventListener {

    private final NotificationFactory factory;
    private final NotificationService service;
    private final RecipientResolver recipients;

    @RabbitListener(queues = RabbitConfig.Q_EMPLOYEE_CREATED)
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIdsByPermission("EMPLOYEE_CREATE");
            for (UUID userId : userIds) {
                var built = factory.fromEmployeeCreated(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed EmployeeCreatedEvent employeeId={}", event.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to process EmployeeCreatedEvent {}: {}", event.getEmployeeId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_EMPLOYEE_TERMINATED)
    public void onEmployeeTerminated(EmployeeTerminatedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIdsByPermission("EMPLOYEE_CREATE");
            for (UUID userId : userIds) {
                var built = factory.fromEmployeeTerminated(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed EmployeeTerminatedEvent employeeId={}", event.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to process EmployeeTerminatedEvent {}: {}", event.getEmployeeId(), e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitConfig.Q_EMPLOYEE_SALARY_CHANGED)
    public void onSalaryChanged(SalaryChangedEvent event) {
        try {
            List<UUID> userIds = recipients.resolveUserIds(event.getEmployeeId());
            for (UUID userId : userIds) {
                var built = factory.fromSalaryChanged(event, userId);
                service.create(built.notification(), built.idempotencyKey(), built.emailRequest());
            }
            log.info("consumed SalaryChangedEvent employeeId={}", event.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to process SalaryChangedEvent {}: {}", event.getEmployeeId(), e.getMessage(), e);
        }
    }
}
