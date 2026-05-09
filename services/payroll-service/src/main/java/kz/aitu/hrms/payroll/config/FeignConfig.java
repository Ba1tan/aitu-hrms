package kz.aitu.hrms.payroll.config;

import feign.RequestInterceptor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableFeignClients(basePackages = "kz.aitu.hrms.payroll.client")
public class FeignConfig {

    /**
     * Forwards the caller's Authorization header (and the gateway-injected
     * X-User-* context headers) onto every outgoing Feign call so downstream
     * services pass their own JwtAuthenticationFilter.
     *
     * Falls through silently when there's no incoming HTTP request — e.g.
     * when a Feign call originates from a RabbitMQ listener or a Spring Batch
     * job. Those paths need a different mechanism (service token), not this.
     */
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