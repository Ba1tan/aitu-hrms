package kz.aitu.hrms.employee.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

/**
 * Resolved data scope for employee queries:
 *   ALL   → no filter (HR / admins / accountants)
 *   TEAM  → only employees managed by the current user (managers)
 *   SELF  → only the current user's own record (regular employees)
 */
@Getter
@AllArgsConstructor
public class EmployeeScope {

    public enum Kind { ALL, TEAM, SELF }

    private final Kind kind;
    private final UUID currentEmployeeId;

    public boolean allows(UUID targetEmployeeId, UUID targetManagerId) {
        return switch (kind) {
            case ALL -> true;
            case TEAM -> currentEmployeeId != null
                    && (currentEmployeeId.equals(targetEmployeeId)
                        || currentEmployeeId.equals(targetManagerId));
            case SELF -> currentEmployeeId != null && currentEmployeeId.equals(targetEmployeeId);
        };
    }
}