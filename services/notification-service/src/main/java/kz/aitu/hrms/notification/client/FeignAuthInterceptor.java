package kz.aitu.hrms.notification.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class FeignAuthInterceptor implements RequestInterceptor {

    @Value("${notification.service-token}")
    private String serviceToken;

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String auth = attrs.getRequest().getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                template.header("Authorization", auth);
                return;
            }
        }
        // Listener path — no HTTP request context, use service token
        template.header("X-Service-Token", serviceToken);
    }
}
