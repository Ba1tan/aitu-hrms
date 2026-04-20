package kz.aitu.hrms.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

public class PositionDtos {

    @Data
    public static class CreatePositionRequest {
        @NotBlank @Size(max = 200)
        private String title;
        private UUID departmentId;
        @DecimalMin(value = "0.00") private BigDecimal minSalary;
        @DecimalMin(value = "0.00") private BigDecimal maxSalary;
        @Size(max = 2000) private String description;
    }

    @Data
    public static class UpdatePositionRequest {
        @Size(max = 200) private String title;
        private UUID departmentId;
        @DecimalMin(value = "0.00") private BigDecimal minSalary;
        @DecimalMin(value = "0.00") private BigDecimal maxSalary;
        @Size(max = 2000) private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionResponse {
        private UUID id;
        private String title;
        private BigDecimal minSalary;
        private BigDecimal maxSalary;
        private String description;
        private DepartmentDtos.DepartmentSummary department;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionSummary {
        private UUID id;
        private String title;
    }
}