package kz.aitu.hrms.user.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountCreatedEvent {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private UUID employeeId;
    private String temporaryPassword;
    private LocalDateTime createdAt;
}