package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Emitted by payroll-service when payslip generation begins for a period.
 * Consumed by notification-service (system-wide "payroll in progress" banner)
 * and reporting-service (audit trail).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollJobStartedEvent {
    private UUID periodId;
    private int year;
    private int month;
    private int employeeCount;
    private LocalDateTime startedAt;
    private UUID startedBy;
}