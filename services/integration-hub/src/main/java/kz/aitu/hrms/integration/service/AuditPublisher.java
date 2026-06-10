package kz.aitu.hrms.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.common.audit.AuditEvents;
import kz.aitu.hrms.integration.config.RabbitConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Emits system-wide audit events ({@code audit.recorded}) consumed by
 * user-service. integration-hub has no general-purpose EventPublisher, so this
 * dedicated component carries the audit trail for settings + sync actions.
 */
@Slf4j
@Component
public class AuditPublisher {

    private static final String SERVICE = "integration-hub";

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    public AuditPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void audit(String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        if (rabbitTemplate == null) {
            log.debug("RabbitTemplate unavailable; skipping audit publish");
            return;
        }
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_AUDIT,
                    AuditEvents.build(SERVICE, action, entityType, entityId, oldValue, newValue, objectMapper));
        } catch (Exception e) {
            log.warn("Failed to publish audit.recorded: {}", e.getMessage());
        }
    }
}
