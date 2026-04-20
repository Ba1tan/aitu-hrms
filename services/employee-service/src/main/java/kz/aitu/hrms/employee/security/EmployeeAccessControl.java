package kz.aitu.hrms.employee.security;

import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Resolves the data-level access scope for the currently authenticated user.
 *
 * Role → Scope mapping (matches docs/HRMS_ENTERPRISE_ARCHITECTURE.md §1.4 RBAC matrix —
 * EMPLOYEE_VIEW_ALL / EMPLOYEE_VIEW_TEAM / EMPLOYEE_VIEW_OWN):
 *   SUPER_ADMIN, DIRECTOR, HR_MANAGER, HR_SPECIALIST → ALL
 *   MANAGER, TEAM_LEAD                               → TEAM (direct reports + self)
 *   ACCOUNTANT, EMPLOYEE                             → SELF
 *
 * The "current employee id" is resolved by matching the JWT email against the
 * employees table — cheap (indexed) and avoids a cross-service call.
 */
@Component
@RequiredArgsConstructor
public class EmployeeAccessControl {

    private static final Set<String> ALL_ROLES =
            Set.of("SUPER_ADMIN", "DIRECTOR", "HR_MANAGER", "HR_SPECIALIST");
    private static final Set<String> TEAM_ROLES = Set.of("MANAGER", "TEAM_LEAD");

    private final EmployeeRepository employeeRepository;

    public EmployeeScope resolveScope() {
        String role = CurrentUser.role();
        UUID employeeId = resolveEmployeeId();
        if (role != null && ALL_ROLES.contains(role)) {
            return new EmployeeScope(EmployeeScope.Kind.ALL, employeeId);
        }
        if (role != null && TEAM_ROLES.contains(role)) {
            return new EmployeeScope(EmployeeScope.Kind.TEAM, employeeId);
        }
        return new EmployeeScope(EmployeeScope.Kind.SELF, employeeId);
    }

    public UUID resolveEmployeeId() {
        String email = CurrentUser.email();
        if (email == null) {
            return null;
        }
        return employeeRepository.findByEmailAndDeletedFalse(email)
                .map(Employee::getId)
                .orElse(null);
    }

    public void assertCanView(Employee target) {
        EmployeeScope scope = resolveScope();
        UUID managerId = target.getManager() != null ? target.getManager().getId() : null;
        if (!scope.allows(target.getId(), managerId)) {
            throw new AccessDeniedException("Not allowed to view this employee");
        }
    }
}