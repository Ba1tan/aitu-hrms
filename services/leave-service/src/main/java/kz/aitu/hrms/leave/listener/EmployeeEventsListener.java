package kz.aitu.hrms.leave.listener;

import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.leave.config.RabbitConfig;
import kz.aitu.hrms.leave.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to {@code employee.created} from employee-service by seeding the new
 * employee's leave balances for the current year against every configured
 * leave type. Idempotent — safe on redelivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeEventsListener {

    private final LeaveBalanceService balanceService;

    @RabbitListener(queues = RabbitConfig.QUEUE_EMPLOYEE_CREATED)
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        if (event == null || event.getEmployeeId() == null) {
            log.warn("Ignoring malformed EmployeeCreatedEvent: {}", event);
            return;
        }
        log.info("Initializing leave balances for new employee {}", event.getEmployeeId());
        try {
            balanceService.initializeForEmployee(event.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to initialize balances for {}: {}",
                    event.getEmployeeId(), e.getMessage(), e);
            throw e; // bubble up so the broker retries / DLQs the message
        }
    }
}