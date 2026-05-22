package kz.aitu.hrms.attendance.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards the caller's JWT and identity headers to downstream services so
 * their JwtAuthenticationFilter accepts the request.
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return;
            }
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader("Authorization");
            if (auth != null && !auth.isBlank()) {
                template.header("Authorization", auth);
            }
            for (String h : new String[]{"X-User-Id", "X-User-Email", "X-User-Role"}) {
                String v = req.getHeader(h);
                if (v != null && !v.isBlank()) {
                    template.header(h, v);
                }
            }
        };
    }
}