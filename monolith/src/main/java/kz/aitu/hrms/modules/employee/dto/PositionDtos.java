package kz.aitu.hrms.modules.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

public class PositionDtos {

    @Data
    public static class CreatePositionRequest {
        @NotBlank(message = "Position title is required")
        @Size(max = 150)
        private String title;

        @Size(max = 500)
        private String description;

        private BigDecimal minSalary;
        private BigDecimal maxSalary;
        private UUID departmentId;
    }

    @Data
    public static class UpdatePositionRequest {
        @Size(max = 150)
        private String title;

        @Size(max = 500)
        private String description;

        private BigDecimal minSalary;
        private BigDecimal maxSalary;
        private UUID departmentId;
    }

    @Data
    public static class PositionResponse {
        private UUID id;
        private String title;
        private String description;
        private BigDecimal minSalary;
        private BigDecimal maxSalary;
        private DepartmentDtos.DepartmentResponse department;
    }
}
