package kz.aitu.hrms.user.event;

import kz.aitu.hrms.common.event.AuditEvent;
import kz.aitu.hrms.user.config.RabbitConfig;
import kz.aitu.hrms.user.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code audit.recorded} events from every other service and persists
 * them into {@code hrms_user.audit_logs} — the single table the admin audit log
 * endpoint reads. user-service's own actions are written directly via
 * {@link AuditService#log}, so they don't round-trip through the broker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditService auditService;

    @RabbitListener(queues = RabbitConfig.Q_AUDIT_RECORDED)
    public void onAuditEvent(AuditEvent event) {
        if (event == null) {
            return;
        }
        try {
            auditService.record(event);
        } catch (Exception ex) {
            // Don't nack into an endless redelivery loop — log and drop.
            log.warn("Failed to persist audit event from {} ({} {}): {}",
                    event.getSourceService(), event.getAction(), event.getEntityType(), ex.getMessage());
        }
    }
}