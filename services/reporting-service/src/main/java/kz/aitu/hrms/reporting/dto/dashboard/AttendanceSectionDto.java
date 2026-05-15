package kz.aitu.hrms.reporting.dto.dashboard;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AttendanceSectionDto implements Serializable {
    private long todayPresent;
    private long todayAbsent;
    private long todayLate;
    private long todayTotal;
    private BigDecimal attendanceRate;
}
