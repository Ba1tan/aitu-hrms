package kz.aitu.hrms.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.aitu.hrms.leave.entity.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class LeaveRequestDtos {

    @Data
    public static class CreateRequest {
        @NotNull
        private UUID leaveTypeId;
        @NotNull
        private LocalDate startDate;
        @NotNull
        private LocalDate endDate;
        @Size(max = 2000)
        private String reason;
    }

    @Data
    public static class ReviewRequest {
        @Size(max = 2000)
        private String comment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeRef {
        private UUID id;
        private String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private EmployeeRef employee;
        private LeaveTypeDtos.Summary leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private int daysRequested;
        private String reason;
        private LeaveStatus status;
        /** The requester's manager who will review this request; null if none assigned. */
        private EmployeeRef approver;
        private UUID reviewedBy;
        private LocalDateTime reviewedAt;
        private String reviewComment;
        private LocalDateTime createdAt;
    }
}