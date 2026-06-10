package kz.aitu.hrms.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kz.aitu.hrms.common.event.AuditEvent;
import kz.aitu.hrms.user.entity.AuditLog;
import kz.aitu.hrms.user.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID userId, String userEmail, String action, String entityType,
                    UUID entityId, Object oldValue, Object newValue) {
        HttpServletRequest req = currentRequest();
        AuditLog entry = AuditLog.builder()
                .userId(userId)
                .userEmail(userEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .ipAddress(req == null ? null : extractIp(req))
                .userAgent(req == null ? null : req.getHeader("User-Agent"))
                .build();
        auditLogRepository.save(entry);
    }

    /**
     * Persist an audit row produced by another service (consumed off
     * {@code audit.recorded}). {@code oldValue}/{@code newValue} arrive already
     * serialized as JSON, so they are stored verbatim. {@code createdAt} is set
     * at persist time (near {@code occurredAt}) by {@code @CreationTimestamp}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) {
        AuditLog entry = AuditLog.builder()
                .userId(event.getActorId())
                .userEmail(event.getActorEmail())
                .action(event.getAction() == null ? "UNKNOWN" : event.getAction())
                .entityType(event.getEntityType() == null ? "UNKNOWN" : event.getEntityType())
                .entityId(event.getEntityId())
                .oldValue(event.getOldValue())
                .newValue(event.getNewValue())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .build();
        auditLogRepository.save(entry);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            log.warn("Audit serialization failed: {}", ex.getMessage());
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String extractIp(HttpServletRequest req) {
        // Prefer X-Real-IP (set by the edge nginx to the real client) over
        // X-Forwarded-For, whose first hop is only correct once every proxy
        // forwards it. Fall back to the socket peer.
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}