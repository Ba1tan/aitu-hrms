package kz.aitu.hrms.reporting.dto.dashboard;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PersonalSectionDto implements Serializable {
    private BigDecimal myLastNetSalary;
    private String myLastPayrollPeriod;
    private BigDecimal myLeaveBalance;
    private String myAttendanceTodayStatus;
}
