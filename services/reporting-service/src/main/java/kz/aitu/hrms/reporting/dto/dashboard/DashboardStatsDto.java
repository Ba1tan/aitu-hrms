package kz.aitu.hrms.reporting.dto.dashboard;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsDto implements Serializable {
    private EmployeesSectionDto employees;
    private PayrollSectionDto lastPayroll;
    private LeaveSectionDto leave;
    private AttendanceSectionDto attendanceToday;
    private PersonalSectionDto personal;
}
