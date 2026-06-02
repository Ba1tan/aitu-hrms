package kz.aitu.hrms.employee.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    /**
     * Forwards the caller's Authorization + X-User-* headers so downstream
     * services pass their own JwtAuthenticationFilter. No-op if the call
     * originates outside a request context (e.g. a scheduled job).
     */
    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
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

    /**
     * Replaces the default Feign decoder so clients unwrap the
     * ApiResponse envelope. See {@link EnvelopeDecoder}.
     */
    @Bean
    public Decoder feignDecoder(ObjectMapper mapper) {
        return new EnvelopeDecoder(mapper);
    }
}