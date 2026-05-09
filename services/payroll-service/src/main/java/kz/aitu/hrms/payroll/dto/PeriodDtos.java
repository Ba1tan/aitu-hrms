package kz.aitu.hrms.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PeriodDtos {

    @Data
    public static class CreateRequest {
        @NotNull
        @Min(value = 2020) @Max(value = 2099)
        private Integer year;

        @NotNull @Min(1) @Max(12)
        private Integer month;

        @NotNull @Min(1) @Max(31)
        private Integer workingDays;
    }

    @Data
    public static class GenerateRequest {
        /** When non-empty, only generate payslips for these employees. */
        private List<UUID> employeeIds;
        /** Run via Spring Batch instead of inline. Forced when employee count is large. */
        private Boolean async;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateResponse {
        private boolean async;
        private Long jobId;
        private Integer generated;
        private Integer skipped;
        private Integer errors;
        private Integer flagged;
        private BigDecimal totalGrossPayout;
        private BigDecimal totalNetPayout;
        private List<String> errorDetails;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private Integer year;
        private Integer month;
        private String name;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer workingDays;
        private PayrollPeriodStatus status;
        private UUID processedBy;
        private LocalDateTime processedAt;
        private UUID approvedBy;
        private LocalDateTime approvedAt;
        private Long batchJobId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Summary summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long payslipCount;
        private long approvedCount;
        private long flaggedCount;
        private BigDecimal totalGrossSalary;
        private BigDecimal totalNetSalary;
        private BigDecimal totalIpn;
        private BigDecimal totalOpv;
        private BigDecimal totalVosms;
        private BigDecimal totalSo;
        private BigDecimal totalSn;
        private BigDecimal totalOpvr;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobStatus {
        private Long jobId;
        private UUID periodId;
        private String status;       // STARTING / STARTED / COMPLETED / FAILED
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private Integer totalEmployees;
        private Integer processed;
    }
}