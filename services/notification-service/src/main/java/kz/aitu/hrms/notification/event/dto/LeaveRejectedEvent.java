package kz.aitu.hrms.notification.event.dto;

import lombok.*;

import java.util.UUID;

/**
 * TODO(hrms-common): move to kz.aitu.hrms.common.event when the producer
 * service (leave-service) is ready to share it. Field shapes must match
 * docs/EVENTS.md §3 exactly — Jackson deserializes by name.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveRejectedEvent {
    private UUID requestId;
    private UUID employeeId;
    private UUID reviewedBy;
    private String comment;
}
