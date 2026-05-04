package kz.aitu.hrms.leave.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRejectedEvent {
    private UUID requestId;
    private UUID employeeId;
    private UUID reviewedBy;
    private String comment;
}