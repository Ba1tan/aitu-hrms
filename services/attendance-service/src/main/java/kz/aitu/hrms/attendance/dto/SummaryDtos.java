package kz.aitu.hrms.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

public class SummaryDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSummary {
        private UUID employeeId;
        private int year;
        private int month;
        private long presentDays;
        private long lateDays;
        private long absentDays;
        private long halfDays;
        private long onLeaveDays;
        private long holidayDays;
        private BigDecimal totalWorkedHours;
        private BigDecimal overtimeHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregateSummary {
        private int year;
        private int month;
        private long presentDays;
        private long lateDays;
        private long absentDays;
        private long halfDays;
        private long onLeaveDays;
        private long holidayDays;
    }
}