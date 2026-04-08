package kz.aitu.hrms.modules.auth.entity;

import jakarta.persistence.*;
import kz.aitu.hrms.common.audit.BaseEntity;
import kz.aitu.hrms.modules.employee.entity.Employee;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountNonLocked = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "require_password_change", nullable = false)
    private boolean requirePasswordChange = false;

    @Column(name = "last_login_at")
    private java.time.LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private java.time.LocalDateTime lockedUntil;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Helper for backward compatibility — returns employee UUID from relationship.
     */
    public java.util.UUID getEmployeeId() {
        return employee != null ? employee.getId() : null;
    }
}
