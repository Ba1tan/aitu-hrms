package kz.aitu.hrms.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Mirrors employee-service {@code EmployeeDtos.EmployeeSummary} (the list-row
 * shape): a single {@code fullName}, with {@code department}/{@code position}
 * already flattened to their names.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeSummaryDto {
    private UUID id;
    private String employeeNumber;
    private String fullName;
    private String email;
    private String department;
    private String position;
    private String status;
    private LocalDate hireDate;
}
