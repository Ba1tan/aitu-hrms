package kz.aitu.hrms.user.config;

import kz.aitu.hrms.user.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class AuditorConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
                return Optional.of("system");
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof User u) {
                return Optional.of(u.getEmail());
            }
            return Optional.of(auth.getName());
        };
    }
}