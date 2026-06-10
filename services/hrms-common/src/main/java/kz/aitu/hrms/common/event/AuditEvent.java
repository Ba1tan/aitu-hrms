package kz.aitu.hrms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * System-wide audit record emitted by any service on a sensitive write and
 * consumed by user-service into {@code hrms_user.audit_logs}. Routing key
 * {@code audit.recorded} on the shared {@code hrms.events} topic exchange.
 *
 * <p>{@code oldValue}/{@code newValue} are pre-serialized JSON strings (or
 * {@code null}) so the producer decides the snapshot shape and the consumer
 * stores them verbatim in the JSONB columns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private UUID actorId;
    private String actorEmail;
    /** CREATE | UPDATE | DELETE | APPROVE | REJECT | CANCEL | PROCESS | PAY | LOCK | TERMINATE | SYNC | … */
    private String action;
    /** EMPLOYEE | DEPARTMENT | POSITION | PAYROLL_PERIOD | PAYSLIP | LEAVE_REQUEST | SETTING | … */
    private String entityType;
    private UUID entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    /** Originating service, e.g. {@code employee-service}. */
    private String sourceService;
    private LocalDateTime occurredAt;
}