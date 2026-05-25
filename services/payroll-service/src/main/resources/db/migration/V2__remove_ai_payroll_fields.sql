-- Remove AI/anomaly fields and FLAGGED status from the payslips table.
-- Safe to apply on a previously V1-initialized database.

ALTER TABLE hrms_payroll.payslips
    DROP COLUMN IF EXISTS anomaly_score,
    DROP COLUMN IF EXISTS anomaly_flags,
    DROP COLUMN IF EXISTS ai_reviewed,
    DROP COLUMN IF EXISTS ai_reviewed_by,
    DROP COLUMN IF EXISTS ai_reviewed_at;

ALTER TABLE hrms_payroll.payslips
    DROP CONSTRAINT IF EXISTS payslips_status_check,
    ADD CONSTRAINT payslips_status_check
        CHECK (status IN ('DRAFT','APPROVED','PAID'));

DROP INDEX IF EXISTS hrms_payroll.idx_payslips_flagged;