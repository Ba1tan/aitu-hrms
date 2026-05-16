package kz.aitu.hrms.reporting.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeSummaryDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String position;
    private String department;
    private UUID departmentId;
    private String status;
    private LocalDate hireDate;
}
