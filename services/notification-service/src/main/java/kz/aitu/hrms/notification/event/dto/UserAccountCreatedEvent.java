package kz.aitu.hrms.notification.event.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TODO(hrms-common): move to kz.aitu.hrms.common.event when the producer
 * service (user-service) is ready to share it. Field shapes must match
 * docs/EVENTS.md §3 exactly — Jackson deserializes by name.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserAccountCreatedEvent {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private UUID employeeId;        // nullable
    private String temporaryPassword;
    private LocalDateTime createdAt;
}
