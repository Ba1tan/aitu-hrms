package kz.aitu.hrms.employee.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    public static AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u)) {
            return null;
        }
        return u;
    }

    public static String role() {
        AuthenticatedUser u = get();
        return u == null ? null : u.getRole();
    }

    public static String email() {
        AuthenticatedUser u = get();
        return u == null ? null : u.getEmail();
    }
}