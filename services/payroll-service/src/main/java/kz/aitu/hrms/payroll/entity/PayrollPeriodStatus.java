package kz.aitu.hrms.payroll.entity;

/**
 * Lifecycle for a payroll period.
 *   DRAFT       — created, not yet generated
 *   PROCESSING  — Spring Batch / sync generation in progress
 *   COMPLETED   — payslips generated, awaiting approval
 *   APPROVED    — HR approved; ready for payment
 *   PAID        — bank transfers issued
 *   LOCKED      — immutable archive (super-admin only)
 */
public enum PayrollPeriodStatus {
    DRAFT,
    PROCESSING,
    COMPLETED,
    APPROVED,
    PAID,
    LOCKED
}