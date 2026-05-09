package kz.aitu.hrms.payroll.listener;

import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.common.event.SalaryChangedEvent;
import kz.aitu.hrms.payroll.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * payroll-service does not maintain its own employee table; the next payslip
 * generation will pull live data from employee-service. These listeners exist
 * for observability and for future use cases (precomputed YTD caches, etc.).
 */
@Slf4j
@Component
public class EmployeeEventsListener {

    @RabbitListener(queues = RabbitConfig.QUEUE_EMPLOYEE_CREATED)
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        if (event == null || event.getEmployeeId() == null) {
            log.warn("Ignoring malformed EmployeeCreatedEvent: {}", event);
            return;
        }
        log.info("Noted new employee for payroll inclusion: {} ({})",
                event.getEmployeeId(), event.getFullName());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_EMPLOYEE_SALARY_CHANGED)
    public void onSalaryChanged(SalaryChangedEvent event) {
        if (event == null || event.getEmployeeId() == null) {
            log.warn("Ignoring malformed SalaryChangedEvent: {}", event);
            return;
        }
        log.info("Salary change for employee {}: {} -> {} effective {}",
                event.getEmployeeId(),
                event.getPreviousSalary(), event.getNewSalary(),
                event.getEffectiveDate());
    }
}