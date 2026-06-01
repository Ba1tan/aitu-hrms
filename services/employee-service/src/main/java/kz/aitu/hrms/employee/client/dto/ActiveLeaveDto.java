package kz.aitu.hrms.employee.client.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Mirror of {@code kz.aitu.hrms.leave.dto.ActiveLeaveDtos.ActiveLeaveDto} —
 * employees currently on approved leave today. Used to derive ON_LEAVE
 * badges in employee responses without writing to employees.status.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ActiveLeaveDto {
    private UUID employeeId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
}