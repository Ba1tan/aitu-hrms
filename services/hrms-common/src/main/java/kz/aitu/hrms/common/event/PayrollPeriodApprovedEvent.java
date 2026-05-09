package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Emitted by payroll-service when HR approves a COMPLETED period. Triggers
 * integration-hub to start the 1C sync and bank-file generation flow.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriodApprovedEvent {
    private UUID periodId;
    private int year;
    private int month;
    private long payslipCount;
    private BigDecimal totalNet;
    private LocalDateTime approvedAt;
    private UUID approvedBy;
}