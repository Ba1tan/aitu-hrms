package kz.aitu.hrms.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class SalaryHistoryDtos {

    @Data
    public static class SalaryChangeRequest {
        @NotNull
        @DecimalMin(value = "0.00")
        private BigDecimal newSalary;

        @NotNull
        private LocalDate effectiveDate;

        @Size(max = 1000)
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryHistoryResponse {
        private UUID id;
        private BigDecimal previousSalary;
        private BigDecimal newSalary;
        private LocalDate effectiveDate;
        private String reason;
        private UUID approvedBy;
        private LocalDateTime createdAt;
    }
}