package kz.aitu.hrms.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kz.aitu.hrms.common.event.AuditEvent;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builds an {@link AuditEvent} from the current request context — the
 * authenticated actor ({@link AuthenticatedUser} principal), client IP and
 * user-agent — so each service only has to describe <em>what</em> changed.
 *
 * <p>Lives in hrms-common (no spring-amqp dependency): services publish the
 * returned event through their own {@code EventPublisher}/{@code RabbitTemplate}.
 */
public final class AuditEvents {

    private static final Logger log = LoggerFactory.getLogger(AuditEvents.class);

    private AuditEvents() {}

    public static AuditEvent build(String sourceService, String action, String entityType,
                                   UUID entityId, Object oldValue, Object newValue,
                                   ObjectMapper mapper) {
        AuthenticatedUser actor = currentActor();
        HttpServletRequest req = currentRequest();
        return AuditEvent.builder()
                .actorId(actor == null ? null : actor.getUserId())
                .actorEmail(actor == null ? null : actor.getEmail())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(toJson(mapper, oldValue))
                .newValue(toJson(mapper, newValue))
                .ipAddress(req == null ? null : extractIp(req))
                .userAgent(req == null ? null : req.getHeader("User-Agent"))
                .sourceService(sourceService)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private static AuthenticatedUser currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return u;
        }
        return null;
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String extractIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static String toJson(ObjectMapper mapper, Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("Audit serialization failed: {}", ex.getMessage());
            return null;
        }
    }
}