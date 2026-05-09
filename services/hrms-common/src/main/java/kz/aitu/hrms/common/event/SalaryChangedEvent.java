package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Published by employee-service after a salary record is committed (POST
 * /employees/{id}/salary-change). payroll-service consumes for next period;
 * notification-service sends an internal notice to the employee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryChangedEvent {
    private UUID employeeId;
    private BigDecimal previousSalary;
    private BigDecimal newSalary;
    private LocalDate effectiveDate;
    private String reason;
    private UUID approvedBy;
}