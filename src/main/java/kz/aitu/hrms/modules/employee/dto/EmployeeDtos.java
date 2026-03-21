package kz.aitu.hrms.modules.employee.dto;

import jakarta.validation.constraints.*;
import kz.aitu.hrms.modules.employee.entity.EmploymentStatus;
import kz.aitu.hrms.modules.employee.entity.EmploymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class EmployeeDtos {

    // ===================== REQUESTS =====================

    @Data
    public static class CreateEmployeeRequest {

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        private String lastName;

        @Size(max = 100)
        private String middleName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @Size(max = 12, min = 12, message = "IIN must be exactly 12 digits")
        @Pattern(regexp = "\\d{12}", message = "IIN must contain only digits")
        private String iin;

        @Size(max = 20)
        private String phone;

        @NotNull(message = "Hire date is required")
        private LocalDate hireDate;

        private LocalDate dateOfBirth;

        @NotNull(message = "Employment type is required")
        private EmploymentType employmentType;

        @NotNull(message = "Base salary is required")
        @DecimalMin(value = "0.01", message = "Salary must be positive")
        private BigDecimal baseSalary;

        private UUID departmentId;

        private UUID positionId;

        private UUID managerId;

        private String bankAccount;

        private String bankName;

        private boolean resident = true;
        private boolean hasDisability = false;
        private boolean pensioner = false;
    }

    @Data
    public static class UpdateEmployeeRequest {

        @Size(max = 100)
        private String firstName;

        @Size(max = 100)
        private String lastName;

        @Size(max = 100)
        private String middleName;

        @Email
        private String email;

        @Size(max = 20)
        private String phone;

        private EmploymentType employmentType;

        @DecimalMin(value = "0.01")
        private BigDecimal baseSalary;

        private UUID departmentId;
        private UUID positionId;
        private UUID managerId;

        private String bankAccount;
        private String bankName;
        private Boolean resident;
        private Boolean hasDisability;
        private Boolean pensioner;
    }

    @Data
    public static class UpdateStatusRequest {
        @NotNull
        private EmploymentStatus status;
        private LocalDate terminationDate;
    }

    @Data
    public static class EmployeeResponse {
        private UUID id;
        private String employeeNumber;
        private String firstName;
        private String lastName;
        private String middleName;
        private String fullName;
        private String email;
        private String iin;
        private String phone;
        private LocalDate hireDate;
        private LocalDate terminationDate;
        private LocalDate dateOfBirth;
        private EmploymentStatus status;
        private EmploymentType employmentType;
        private BigDecimal baseSalary;
        private DepartmentSummary department;
        private PositionSummary position;
        private ManagerSummary manager;
        private String bankAccount;
        private String bankName;
        private boolean resident;
        private boolean hasDisability;
        private boolean pensioner;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class EmployeeSummary {
        private UUID id;
        private String employeeNumber;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private EmploymentStatus status;
        private DepartmentSummary department;
        private PositionSummary position;
    }

    @Data
    public static class DepartmentSummary {
        private UUID id;
        private String name;
    }

    @Data
    public static class PositionSummary {
        private UUID id;
        private String title;
    }

    @Data
    public static class ManagerSummary {
        private UUID id;
        private String fullName;
        private String email;
    }
}
