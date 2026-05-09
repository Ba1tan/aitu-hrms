package kz.aitu.hrms.payroll.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.aitu.hrms.payroll.entity.AdditionCategory;
import kz.aitu.hrms.payroll.entity.AdditionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AdditionDtos {

    @Data
    public static class CreateRequest {
        @NotNull private UUID employeeId;
        @NotNull private UUID periodId;
        @NotNull private AdditionType type;
        @NotNull private AdditionCategory category;
        @Size(max = 255) private String description;

        @NotNull @DecimalMin(value = "0.00")
        private BigDecimal amount;

        private Boolean isTaxable;
    }

    @Data
    public static class UpdateRequest {
        private AdditionType type;
        private AdditionCategory category;
        @Size(max = 255) private String description;
        @DecimalMin(value = "0.00") private BigDecimal amount;
        private Boolean isTaxable;
    }

    @Data
    public static class BulkRequest {
        @NotNull private UUID periodId;
        @NotNull private List<UUID> employeeIds;
        @NotNull private AdditionType type;
        @NotNull private AdditionCategory category;
        @Size(max = 255) private String description;
        @NotNull @DecimalMin(value = "0.00") private BigDecimal amount;
        private Boolean isTaxable;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID employeeId;
        private UUID periodId;
        private AdditionType type;
        private AdditionCategory category;
        private String description;
        private BigDecimal amount;
        private boolean isTaxable;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkResponse {
        private int created;
        private List<UUID> ids;
    }
}