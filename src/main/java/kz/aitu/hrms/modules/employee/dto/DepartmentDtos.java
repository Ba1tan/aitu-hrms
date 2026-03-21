package kz.aitu.hrms.modules.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

public class DepartmentDtos {

    @Data
    public static class CreateDepartmentRequest {
        @NotBlank(message = "Department name is required")
        @Size(max = 150)
        private String name;

        @Size(max = 500)
        private String description;

        @Size(max = 50)
        private String costCenter;

        private UUID parentId;
        private UUID managerId;
    }

    @Data
    public static class UpdateDepartmentRequest {
        @Size(max = 150)
        private String name;

        @Size(max = 500)
        private String description;

        @Size(max = 50)
        private String costCenter;

        private UUID parentId;
        private UUID managerId;
    }

    @Data
    public static class DepartmentResponse {
        private UUID id;
        private String name;
        private String description;
        private String costCenter;
        private ParentDepartment parent;
        private ManagerInfo manager;
        private int employeeCount;
    }

    @Data
    public static class ParentDepartment {
        private UUID id;
        private String name;
    }

    @Data
    public static class ManagerInfo {
        private UUID id;
        private String fullName;
        private String email;
    }
}
