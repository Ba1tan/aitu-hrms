package kz.aitu.hrms.employee.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeScopeTest {

    private final UUID me = UUID.randomUUID();
    private final UUID teammate = UUID.randomUUID();
    private final UUID otherEmployee = UUID.randomUUID();

    @Test
    void allScopeAllowsAnyTarget() {
        EmployeeScope scope = new EmployeeScope(EmployeeScope.Kind.ALL, me);
        assertThat(scope.allows(otherEmployee, null)).isTrue();
        assertThat(scope.allows(otherEmployee, UUID.randomUUID())).isTrue();
    }

    @Test
    void teamScopeAllowsDirectReportsAndSelf() {
        EmployeeScope scope = new EmployeeScope(EmployeeScope.Kind.TEAM, me);
        assertThat(scope.allows(teammate, me)).isTrue();   // direct report
        assertThat(scope.allows(me, null)).isTrue();        // self
    }

    @Test
    void teamScopeRejectsNonReport() {
        EmployeeScope scope = new EmployeeScope(EmployeeScope.Kind.TEAM, me);
        assertThat(scope.allows(otherEmployee, UUID.randomUUID())).isFalse();
        assertThat(scope.allows(otherEmployee, null)).isFalse();
    }

    @Test
    void selfScopeOnlyAllowsSelf() {
        EmployeeScope scope = new EmployeeScope(EmployeeScope.Kind.SELF, me);
        assertThat(scope.allows(me, null)).isTrue();
        assertThat(scope.allows(otherEmployee, null)).isFalse();
        assertThat(scope.allows(otherEmployee, me)).isFalse();
    }

    @Test
    void scopesWithNoCurrentEmployeeIdDenyNonAllScopes() {
        EmployeeScope team = new EmployeeScope(EmployeeScope.Kind.TEAM, null);
        EmployeeScope self = new EmployeeScope(EmployeeScope.Kind.SELF, null);
        assertThat(team.allows(teammate, null)).isFalse();
        assertThat(self.allows(teammate, null)).isFalse();
    }
}