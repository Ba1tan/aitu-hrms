-- V3 — Remove vestigial settings that no code consumes.
--   * attendance.require_face      — face/biometric check-in was removed from the product.
--   * payroll.payslip_release_day  — never read; payslip visibility is driven by PayslipStatus
--                                    (DRAFT → APPROVED → PAID), not a calendar day.
-- (V2 still inserts them on a fresh DB; this runs right after and cleans them up.)

DELETE FROM hrms_integration.company_settings
WHERE key IN ('attendance.require_face', 'payroll.payslip_release_day');
