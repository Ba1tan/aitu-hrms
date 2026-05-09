package kz.aitu.hrms.payroll.controller;

import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.common.security.JwtAuthenticationFilter;
import kz.aitu.hrms.payroll.config.SecurityConfig;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.UUID;

/**
 * Shared building blocks for {@code @WebMvcTest} slices:
 *   - imports SecurityConfig so endpoints actually go through @PreAuthorize
 *   - stubs the common {@link JwtAuthenticationFilter} (which would otherwise
 *     fail to wire because it needs a JwtTokenValidator)
 *   - exposes {@link WithMockJwtUser} for declarative auth setup
 */
@TestConfiguration
@Import(SecurityConfig.class)
public class WebMvcTestSupport {

    /**
     * Replace the real JWT filter with a no-op so MockMvc requests don't need
     * a real Bearer token; @WithMockJwtUser fills the SecurityContext directly.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        JwtAuthenticationFilter filter = Mockito.mock(JwtAuthenticationFilter.class);
        try {
            Mockito.doAnswer(inv -> {
                jakarta.servlet.FilterChain chain = inv.getArgument(2);
                chain.doFilter(inv.getArgument(0), inv.getArgument(1));
                return null;
            }).when(filter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return filter;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @WithSecurityContext(factory = WithMockJwtUserSecurityContextFactory.class)
    public @interface WithMockJwtUser {
        String userId() default "00000000-0000-0000-0000-000000000001";
        String employeeId() default "";
        String email() default "test@hrms.kz";
        String role() default "HR_MANAGER";
        String[] authorities() default {};
    }

    public static class WithMockJwtUserSecurityContextFactory
            implements WithSecurityContextFactory<WithMockJwtUser> {
        @Override
        public SecurityContext createSecurityContext(WithMockJwtUser anno) {
            UUID userId = UUID.fromString(anno.userId());
            UUID empId = anno.employeeId().isBlank() ? null : UUID.fromString(anno.employeeId());
            AuthenticatedUser principal = new AuthenticatedUser(userId, anno.email(), anno.role(), empId);

            var authorities = Arrays.stream(anno.authorities())
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            return ctx;
        }
    }
}