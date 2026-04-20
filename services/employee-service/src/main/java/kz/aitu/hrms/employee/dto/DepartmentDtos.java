package kz.aitu.hrms.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class DepartmentDtos {

    @Data
    public static class CreateDepartmentRequest {
        @NotBlank @Size(max = 200)
        private String name;
        @Size(max = 50)
        private String code;
        @Size(max = 2000)
        private String description;
        private UUID parentId;
        private UUID managerId;
    }

    @Data
    public static class UpdateDepartmentRequest {
        @Size(max = 200) private String name;
        @Size(max = 50)  private String code;
        @Size(max = 2000) private String description;
        private UUID parentId;
        private UUID managerId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentResponse {
        private UUID id;
        private String name;
        private String code;
        private String description;
        private DepartmentSummary parent;
        private EmployeeDtos.ManagerSummary manager;
        private long employeeCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private UUID id;
        private String name;
    }
}