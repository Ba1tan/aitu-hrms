package kz.aitu.hrms.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Principal populated by {@link kz.aitu.hrms.common.security.JwtAuthenticationFilter}
 * from the incoming JWT. Consumer services do not own the user table — they trust
 * user-service's signed token.
 *
 * {@code employeeId} is the optional claim added by user-service when the user is
 * linked to an employee profile. System accounts and super-admins may have none —
 * callers must handle a {@code null} {@code employeeId}.
 */
@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private final UUID userId;
    private final String email;
    private final String role;
    private final UUID employeeId;
}