package kz.aitu.hrms.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Lightweight per-employee summary of "who is currently on approved leave".
 * Used by employee-service to derive an employee's display status (so the
 * profile badge reads ON_LEAVE while the leave is active even though
 * employees.status is never written by the leave flow), and by
 * reporting-service for the dashboard's "В отпуске" counter.
 */
public final class ActiveLeaveDtos {
    private ActiveLeaveDtos() {}

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActiveLeaveDto {
        private UUID employeeId;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ActiveLeaveCountDto {
        private long count;
    }
}