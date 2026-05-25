package kz.aitu.hrms.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mirrors payroll-service {@code PayslipDtos.Response}: employee and period are
 * nested objects, the employee carries a single {@code fullName}, and the tax
 * amounts use the {@code *Amount} suffix. The convenience accessors
 * ({@link #getEmployeeName()}, {@link #getPeriodName()}, {@link #getEmployeeId()})
 * flatten those for report writers.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayslipDto {
    private UUID id;
    private EmployeeInfo employee;
    private PeriodInfo period;

    private BigDecimal grossSalary;
    private BigDecimal opvAmount;
    private BigDecimal vosmsAmount;
    private BigDecimal ipnAmount;
    private BigDecimal netSalary;

    public String getEmployeeName() {
        return employee != null ? employee.getFullName() : null;
    }

    public UUID getEmployeeId() {
        return employee != null ? employee.getId() : null;
    }

    public String getPeriodName() {
        return period != null ? period.getName() : null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmployeeInfo {
        private UUID id;
        private String fullName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PeriodInfo {
        private UUID id;
        private String name;
    }
}
