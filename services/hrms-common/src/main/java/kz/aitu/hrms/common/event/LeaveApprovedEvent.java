package kz.aitu.hrms.common.event;

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
public class LeaveApprovedEvent {
    private UUID requestId;
    private UUID employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
}
