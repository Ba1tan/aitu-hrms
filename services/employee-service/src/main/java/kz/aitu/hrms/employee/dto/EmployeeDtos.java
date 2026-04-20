package kz.aitu.hrms.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kz.aitu.hrms.employee.entity.DisabilityGroup;
import kz.aitu.hrms.employee.entity.EmploymentStatus;
import kz.aitu.hrms.employee.entity.EmploymentType;
import kz.aitu.hrms.employee.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class EmployeeDtos {

    @Data
    public static class CreateEmployeeRequest {
        @NotBlank @Size(max = 100)
        private String firstName;

        @NotBlank @Size(max = 100)
        private String lastName;

        @Size(max = 100)
        private String middleName;

        @Email @Size(max = 255)
        private String email;

        @Pattern(regexp = "\\d{12}", message = "IIN must be 12 digits")
        private String iin;

        @Size(max = 20)
        private String phone;

        @NotNull
        private LocalDate hireDate;

        private LocalDate dateOfBirth;

        private Gender gender;

        private EmploymentType employmentType;

        @NotNull @DecimalMin(value = "0.00")
        private BigDecimal baseSalary;

        private UUID departmentId;
        private UUID positionId;
        private UUID managerId;

        private DisabilityGroup disabilityGroup;
        private Boolean isResident;
        private Boolean isPensioner;

        @Size(max = 1000)
        private String address;

        private Boolean createAccount;
    }

    @Data
    public static class UpdateEmployeeRequest {
        @Size(max = 100) private String firstName;
        @Size(max = 100) private String lastName;
        @Size(max = 100) private String middleName;
        @Email @Size(max = 255) private String email;
        @Pattern(regexp = "\\d{12}") private String iin;
        @Size(max = 20) private String phone;
        private LocalDate dateOfBirth;
        private Gender gender;
        private EmploymentType employmentType;
        @DecimalMin(value = "0.00") private BigDecimal baseSalary;
        private UUID departmentId;
        private UUID positionId;
        private UUID managerId;
        private DisabilityGroup disabilityGroup;
        private Boolean isResident;
        private Boolean isPensioner;
        @Size(max = 1000) private String address;
    }

    @Data
    public static class UpdateStatusRequest {
        @NotNull
        private EmploymentStatus status;
        @Size(max = 500)
        private String reason;
    }

    @Data
    public static class TerminateRequest {
        @NotNull
        private LocalDate terminationDate;
        @NotBlank @Size(max = 500)
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
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
        private LocalDate dateOfBirth;
        private Gender gender;
        private LocalDate hireDate;
        private LocalDate terminationDate;
        private String terminationReason;
        private EmploymentStatus status;
        private EmploymentType employmentType;
        private BigDecimal baseSalary;
        private DisabilityGroup disabilityGroup;
        private boolean isResident;
        private boolean isPensioner;
        private String address;
        private DepartmentDtos.DepartmentSummary department;
        private PositionDtos.PositionSummary position;
        private ManagerSummary manager;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSummary {
        private UUID id;
        private String employeeNumber;
        private String fullName;
        private String email;
        private String department;
        private String position;
        private EmploymentStatus status;
        private LocalDate hireDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerSummary {
        private UUID id;
        private String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrgChartNode {
        private UUID id;
        private String employeeNumber;
        private String fullName;
        private String email;
        private String position;
        private String department;
        private java.util.List<OrgChartNode> reports;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportResult {
        private int totalRows;
        private int imported;
        private int skipped;
        private java.util.List<String> errors;
    }
}