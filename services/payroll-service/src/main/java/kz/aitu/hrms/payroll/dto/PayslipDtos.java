package kz.aitu.hrms.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PayslipDtos {

    @Data
    public static class AdjustRequest {
        @DecimalMin(value = "0.00")
        private BigDecimal allowances;

        @DecimalMin(value = "0.00")
        private BigDecimal otherDeductions;

        @Min(value = 0) @Max(value = 31)
        private Integer workedDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private PeriodInfo period;
        private EmployeeInfo employee;

        private Integer workedDays;
        private Integer totalWorkingDays;

        private BigDecimal grossSalary;
        private BigDecimal earnedSalary;
        private BigDecimal allowances;
        private BigDecimal otherDeductions;

        private BigDecimal opvAmount;
        private BigDecimal vosmsAmount;
        private BigDecimal oopvAmount;
        private BigDecimal taxableIncome;
        private BigDecimal ipnAmount;
        private BigDecimal totalDeductions;
        private BigDecimal netSalary;

        private BigDecimal soAmount;
        private BigDecimal snAmount;
        private BigDecimal opvrAmount;

        private Integer mrpUsed;
        private boolean isResident;
        private boolean hasDisability;

        private PayslipStatus status;
        private BigDecimal anomalyScore;
        private List<String> anomalyFlags;
        private boolean aiReviewed;
        private UUID aiReviewedBy;
        private LocalDateTime aiReviewedAt;

        private String pdfUrl;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodInfo {
        private UUID id;
        private Integer year;
        private Integer month;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeInfo {
        private UUID id;
        private String employeeNumber;
        private String fullName;
        private String iin;
        private String department;
        private String position;
    }
}