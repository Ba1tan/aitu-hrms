package kz.aitu.hrms.leave.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestCreatedEvent {
    private UUID requestId;
    private UUID employeeId;
    private UUID managerId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int daysRequested;
}