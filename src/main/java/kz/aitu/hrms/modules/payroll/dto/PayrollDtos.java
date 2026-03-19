package kz.aitu.hrms.modules.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kz.aitu.hrms.modules.payroll.enums.PayrollPeriodStatus;
import kz.aitu.hrms.modules.payroll.enums.PayslipStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PayrollDtos {

    // ===================== PERIOD REQUESTS =====================

    @Data
    public static class CreatePeriodRequest {

        @NotNull(message = "Year is required")
        @Min(value = 2020, message = "Year must be 2020 or later")
        @Max(value = 2099)
        private Integer year;

        @NotNull(message = "Month is required")
        @Min(value = 1) @Max(value = 12)
        private Integer month;

        @NotNull(message = "Working days count is required")
        @Min(value = 1) @Max(value = 31)
        private Integer workingDays;
    }

    // ===================== GENERATE PAYSLIPS REQUEST =====================

    @Data
    public static class GeneratePayslipsRequest {
        // If provided, only generate for these specific employee IDs.
        // If empty/null → generate for ALL active employees.
        private List<UUID> employeeIds;
    }

    // ===================== PAYSLIP ADJUSTMENT =====================

    @Data
    public static class AdjustPayslipRequest {
        @DecimalMin(value = "0.00")
        private BigDecimal allowances;       // Extra bonuses/allowances

        @DecimalMin(value = "0.00")
        private BigDecimal otherDeductions;  // Manual deductions

        @Min(value = 0) @Max(value = 31)
        private Integer workedDays;          // Override prorated days
    }

    // ===================== PERIOD RESPONSES =====================

    @Data
    public static class PeriodResponse {
        private UUID id;
        private Integer year;
        private Integer month;
        private String name;           // e.g. "Март 2024"
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer workingDays;
        private PayrollPeriodStatus status;
        private PeriodSummary summary; // nullable until payslips generated
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class PeriodSummary {
        private long payslipCount;
        private long approvedCount;
        private BigDecimal totalGrossSalary;
        private BigDecimal totalNetSalary;
        private BigDecimal totalIpn;
        private BigDecimal totalOpv;
        private BigDecimal totalSo;
    }

    // ===================== PAYSLIP RESPONSES =====================

    @Data
    public static class PayslipResponse {
        private UUID id;
        private PeriodInfo period;
        private EmployeeInfo employee;

        // Days
        private Integer workedDays;
        private Integer totalWorkingDays;

        // Earnings
        private BigDecimal grossSalary;
        private BigDecimal earnedSalary;
        private BigDecimal allowances;

        // Employee deductions
        private BigDecimal opvAmount;
        private BigDecimal oopvAmount;
        private BigDecimal vosmsAmount;
        private BigDecimal opvrAmount;
        private BigDecimal taxableIncome;
        private BigDecimal ipnAmount;
        private BigDecimal otherDeductions;
        private BigDecimal totalDeductions;

        // Take-home
        private BigDecimal netSalary;

        // Employer contributions (informational)
        private BigDecimal soAmount;
        private BigDecimal snAmount;

        // Metadata
        private Integer mrpUsed;
        private boolean resident;
        private PayslipStatus status;
        private String pdfUrl;
        private LocalDateTime createdAt;
    }

    @Data
    public static class GeneratePayslipsResponse {
        private int generated;
        private int skipped;     // already existed
        private int errors;
        private BigDecimal totalGrossPayout;
        private BigDecimal totalNetPayout;
        private List<String> errorDetails;
    }

    @Data
    public static class PeriodInfo {
        private UUID id;
        private String name;
        private Integer year;
        private Integer month;
    }

    @Data
    public static class EmployeeInfo {
        private UUID id;
        private String employeeNumber;
        private String fullName;
        private String email;
        private String department;
        private String position;
    }
}
