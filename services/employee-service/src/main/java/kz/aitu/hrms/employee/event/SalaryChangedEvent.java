package kz.aitu.hrms.employee.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
}