package kz.aitu.hrms.employee.security;

import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeAccessControlTest {

    private final EmployeeRepository repo = mock(EmployeeRepository.class);
    private final EmployeeAccessControl access = new EmployeeAccessControl(repo);
    private final UUID currentEmployeeId = UUID.randomUUID();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authAs(String role) {
        Employee e = new Employee();
        e.setId(currentEmployeeId);
        when(repo.findByEmailAndDeletedFalse(anyString())).thenReturn(Optional.of(e));
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "user@example.com", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUPER_ADMIN", "DIRECTOR", "HR_MANAGER", "HR_SPECIALIST"})
    void rolesWithViewAllResolveToAllScope(String role) {
        authAs(role);
        assertThat(access.resolveScope().getKind()).isEqualTo(EmployeeScope.Kind.ALL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "TEAM_LEAD"})
    void rolesWithViewTeamResolveToTeamScope(String role) {
        authAs(role);
        assertThat(access.resolveScope().getKind()).isEqualTo(EmployeeScope.Kind.TEAM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCOUNTANT", "EMPLOYEE"})
    void rolesWithoutTeamOrAllResolveToSelfScope(String role) {
        authAs(role);
        assertThat(access.resolveScope().getKind()).isEqualTo(EmployeeScope.Kind.SELF);
    }

    @Test
    void noAuthenticationResolvesToSelfWithNullEmployeeId() {
        EmployeeScope scope = access.resolveScope();
        assertThat(scope.getKind()).isEqualTo(EmployeeScope.Kind.SELF);
        assertThat(scope.getCurrentEmployeeId()).isNull();
    }
}