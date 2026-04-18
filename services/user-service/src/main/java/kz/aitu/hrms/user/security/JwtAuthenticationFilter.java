package kz.aitu.hrms.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.aitu.hrms.user.entity.User;
import kz.aitu.hrms.user.repository.RolePermissionRepository;
import kz.aitu.hrms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

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
            if (blacklist.isBlacklisted(token) || !jwtService.isValid(token) || !jwtService.isAccessToken(token)) {
                chain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UUID userId = UUID.fromString(jwtService.extractUserId(token));
                userRepository.findById(userId).ifPresent(user -> attachAuthentication(request, user));
            }
        } catch (Exception ex) {
            log.warn("JWT filter error: {}", ex.getMessage());
        }

        chain.doFilter(request, response);
    }

    private void attachAuthentication(HttpServletRequest request, User user) {
        user.setPermissionCodes(rolePermissionRepository.findPermissionCodesByRole(user.getRole()));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}