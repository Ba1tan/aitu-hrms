package kz.aitu.hrms.payroll.entity;

/**
 * Per-payslip status. {@code FLAGGED} is set by the AI anomaly detector and
 * blocks period approval until cleared via {@code approve-flagged}.
 */
public enum PayslipStatus {
    DRAFT,
    FLAGGED,
    APPROVED,
    PAID
}