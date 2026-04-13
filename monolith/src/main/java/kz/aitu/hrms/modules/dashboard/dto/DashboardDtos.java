package kz.aitu.hrms.modules.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

public class DashboardDtos {


    @Data
    @Builder
    public static class DashboardStatsResponse {


        private Integer totalEmployees;
        private Integer activeEmployees;
        private Integer onLeaveEmployees;
        private Integer newHiresThisMonth;


        private String lastPayrollPeriodName;
        private String lastPayrollStatus;
        private BigDecimal lastPayrollGross;
        private BigDecimal lastPayrollNet;
        private Integer lastPayrollEmployeeCount;


        private Integer pendingLeaveRequests;


        private Integer todayPresent;
        private Integer todayAbsent;
        private Integer todayLate;
        private Integer todayTotal;
        private Double attendanceRate;


        private BigDecimal myLastNetSalary;
        private String myLastPayrollPeriod;
        private Integer myLeaveBalance;
        private String myAttendanceTodayStatus;
    }
}
