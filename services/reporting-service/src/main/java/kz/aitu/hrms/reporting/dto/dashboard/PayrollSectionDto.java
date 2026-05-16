package kz.aitu.hrms.reporting.dto.dashboard;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayrollSectionDto implements Serializable {
    private String lastPayrollPeriodName;
    private String lastPayrollStatus;
    private BigDecimal lastPayrollGross;
    private BigDecimal lastPayrollNet;
    private long lastPayrollEmployeeCount;
}
