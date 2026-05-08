package kz.aitu.hrms.leave.security;

import kz.aitu.hrms.common.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {}

    public static AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            return null;
        }
        return u;
    }

    public static UUID userId() {
        AuthenticatedUser u = get();
        return u == null ? null : u.getUserId();
    }

    public static UUID employeeId() {
        AuthenticatedUser u = get();
        return u == null ? null : u.getEmployeeId();
    }

    public static String email() {
        AuthenticatedUser u = get();
        return u == null ? null : u.getEmail();
    }

    public static boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) return true;
        }
        return false;
    }
}