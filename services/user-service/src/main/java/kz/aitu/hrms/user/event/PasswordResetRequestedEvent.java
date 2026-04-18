package kz.aitu.hrms.user.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestedEvent {
    private UUID userId;
    private String email;
    private String firstName;
    private String resetToken;
    private long ttlSeconds;
}