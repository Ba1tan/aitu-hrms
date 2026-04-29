package kz.aitu.hrms.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.aitu.hrms.common.jwt.JwtTokenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared JWT filter for consumer services (everything except user-service, which
 * has its own filter that does DB lookup + Redis blacklist). Validates the token
 * issued by user-service and populates the SecurityContext with an
 * {@link AuthenticatedUser} principal carrying userId/email/role/employeeId.
 *
 * Permissions from the {@code permissions} claim become {@link SimpleGrantedAuthority}
 * entries verbatim (e.g. {@code ATTENDANCE_MANAGE}); the role becomes {@code ROLE_<role>}.
 *
 * Endpoints permitted by SecurityConfig (actuator, swagger, kiosk) flow through
 * this filter unchanged when no Authorization header is present.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator validator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            if (!validator.isValid(token)) {
                chain.doFilter(request, response);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = UUID.fromString(validator.extractUserId(token));
                String email = validator.extractEmail(token);
                String role = validator.extractRole(token);
                List<String> permissions = validator.extractPermissions(token);
                UUID employeeId = validator.extractEmployeeId(token);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>(permissions.size() + 1);
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));

                AuthenticatedUser principal = new AuthenticatedUser(userId, email, role, employeeId);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ex) {
            log.warn("JWT filter error: {}", ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}