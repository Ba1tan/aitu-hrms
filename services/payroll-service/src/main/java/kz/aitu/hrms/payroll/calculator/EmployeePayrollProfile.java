package kz.aitu.hrms.payroll.calculator;

import kz.aitu.hrms.common.types.DisabilityGroup;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Self-contained input to {@link KazakhstanPayrollCalculator}. Decouples the
 * calculator from any Feign DTO so unit tests don't need Spring or HTTP
 * stubs, and so that an upstream change to employee-service shape doesn't
 * cascade into a tax-engine change.
 */
@Getter
@Builder
public class EmployeePayrollProfile {
    private UUID employeeId;
    private String employeeNumber;
    private String iin;
    private String fullName;
    private String departmentName;
    private String positionTitle;

    private BigDecimal baseSalary;
    private boolean resident;
    private boolean pensioner;
    private boolean disabled;
    /** Group 1/2 → 5000×МРП deduction; Group 3 → 882×МРП; null/NONE → 0. */
    private DisabilityGroup disabilityGroup;
}