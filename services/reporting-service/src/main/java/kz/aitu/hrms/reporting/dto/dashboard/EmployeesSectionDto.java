package kz.aitu.hrms.reporting.dto.dashboard;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EmployeesSectionDto implements Serializable {
    private long totalEmployees;
    private long activeEmployees;
    private long onLeaveEmployees;
    private long newHiresThisMonth;
}
