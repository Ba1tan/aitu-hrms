package kz.aitu.hrms.employee.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Principal populated by {@link JwtAuthenticationFilter} from the incoming JWT.
 * Employee-service does not own the user table — it trusts user-service's signed token.
 */
@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private final UUID userId;
    private final String email;
    private final String role;
}