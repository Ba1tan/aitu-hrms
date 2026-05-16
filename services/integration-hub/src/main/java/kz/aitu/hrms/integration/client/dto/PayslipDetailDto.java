package kz.aitu.hrms.integration.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayslipDetailDto {

    private UUID id;
    private EmployeeInfo employee;
    private PeriodInfo period;

    private Integer workedDays;
    private Integer totalWorkingDays;

    private BigDecimal grossSalary;
    private BigDecimal earnedSalary;
    private BigDecimal opvAmount;
    private BigDecimal vosmsAmount;
    private BigDecimal ipnAmount;
    private BigDecimal netSalary;
    private BigDecimal soAmount;
    private BigDecimal snAmount;
    private BigDecimal opvrAmount;

    private boolean isResident;
    private boolean hasDisability;

    public UUID getEmployeeId() {
        return employee != null ? employee.getId() : null;
    }

    public String getEmployeeIin() {
        return employee != null ? employee.getIin() : null;
    }

    public String getFullName() {
        return employee != null ? employee.getFullName() : null;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmployeeInfo {
        private UUID id;
        private String fullName;
        private String iin;
    }

    @Getter @Setter @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PeriodInfo {
        private UUID id;
        private Integer year;
        private Integer month;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
