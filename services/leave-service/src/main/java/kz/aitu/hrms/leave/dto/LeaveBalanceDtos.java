package kz.aitu.hrms.leave.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class LeaveBalanceDtos {

    @Data
    public static class InitializeRequest {
        @NotNull
        private Integer year;
    }

    @Data
    public static class AdjustRequest {
        @NotNull
        private Integer days;
        @NotBlank
        @Size(max = 2000)
        private String reason;
    }

    @Data
    public static class CarryoverRequest {
        @NotNull
        private Integer fromYear;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private UUID employeeId;
        private String employeeName;
        private LeaveTypeDtos.Summary leaveType;
        private int year;
        private int entitledDays;
        private int carriedOver;
        private int usedDays;
        private int adjustedDays;
        private int remainingDays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitializeResponse {
        private int year;
        private int employeesProcessed;
        private int balancesCreated;
        private int balancesSkipped;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CarryoverResponse {
        private int fromYear;
        private int toYear;
        private int balancesProcessed;
        private int totalDaysCarried;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private UUID departmentId;
        private int year;
        private List<Response> balances;
    }
}