-- V7 — Align report/AI permission codes to docs/PERMISSIONS.md (the
-- authoritative catalog). reporting-service's @PreAuthorize annotations and
-- the Phase 5 frontend gate on REPORT_PAYROLL / REPORT_ATTENDANCE /
-- REPORT_LEAVE / REPORT_HR / REPORT_EXECUTIVE / AI_DASHBOARD, but V2 only
-- seeded the older synonyms (REPORT_FINANCIAL / REPORT_OPERATIONAL /
-- REPORT_FORM_200 / AI_ANOMALY / AI_FORECAST). Without the canonical codes
-- every report endpoint 403s for non-SUPER_ADMIN users (and Form 200 even
-- for SUPER_ADMIN, since the seed only had REPORT_FORM_200 while the catalog
-- defines REPORT_PAYROLL).
--
-- Same idempotent, additive approach as V5: the old codes stay in the table
-- (removing them would orphan existing role_permissions rows). They are no
-- longer referenced by any controller after this change.

INSERT INTO permissions (code, description, module) VALUES
    ('REPORT_PAYROLL',    'Payroll reports, Form 200.00, salary breakdown', 'report'),
    ('REPORT_ATTENDANCE', 'Attendance monthly / summary reports',           'report'),
    ('REPORT_LEAVE',      'Leave balances report',                          'report'),
    ('REPORT_HR',         'Employee directory, headcount, turnover',        'report'),
    ('AI_DASHBOARD',      'View AI insights, attrition, anomalies',         'ai')
ON CONFLICT (code) DO NOTHING;
-- REPORT_EXECUTIVE already exists from V2.

-- SUPER_ADMIN — re-run the catch-all so the new codes land
INSERT INTO role_permissions (role, permission_id)
SELECT 'SUPER_ADMIN', id FROM permissions WHERE code IN (
    'REPORT_PAYROLL','REPORT_ATTENDANCE','REPORT_LEAVE','REPORT_HR','AI_DASHBOARD'
)
ON CONFLICT DO NOTHING;

-- Role grants per docs/PERMISSIONS.md §3 (canonical seed matrix):
--   DIRECTOR      → REPORT_EXECUTIVE, REPORT_PAYROLL, REPORT_HR, AI_DASHBOARD
--   HR_MANAGER    → all REPORT_*, AI_DASHBOARD
--   HR_SPECIALIST → REPORT_ATTENDANCE, REPORT_LEAVE, REPORT_HR
--   ACCOUNTANT    → REPORT_PAYROLL
--   MANAGER / TEAM_LEAD / EMPLOYEE → none

-- REPORT_PAYROLL → DIRECTOR, HR_MANAGER, ACCOUNTANT
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('DIRECTOR'),('HR_MANAGER'),('ACCOUNTANT')) AS roles(r)
WHERE code = 'REPORT_PAYROLL'
ON CONFLICT DO NOTHING;

-- REPORT_ATTENDANCE → HR_MANAGER, HR_SPECIALIST
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('HR_MANAGER'),('HR_SPECIALIST')) AS roles(r)
WHERE code = 'REPORT_ATTENDANCE'
ON CONFLICT DO NOTHING;

-- REPORT_LEAVE → HR_MANAGER, HR_SPECIALIST
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('HR_MANAGER'),('HR_SPECIALIST')) AS roles(r)
WHERE code = 'REPORT_LEAVE'
ON CONFLICT DO NOTHING;

-- REPORT_HR → DIRECTOR, HR_MANAGER, HR_SPECIALIST
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('DIRECTOR'),('HR_MANAGER'),('HR_SPECIALIST')) AS roles(r)
WHERE code = 'REPORT_HR'
ON CONFLICT DO NOTHING;

-- REPORT_EXECUTIVE → HR_MANAGER (DIRECTOR + SUPER_ADMIN already have it via V2)
INSERT INTO role_permissions (role, permission_id)
SELECT 'HR_MANAGER', id FROM permissions
WHERE code = 'REPORT_EXECUTIVE'
ON CONFLICT DO NOTHING;

-- AI_DASHBOARD → DIRECTOR, HR_MANAGER
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('DIRECTOR'),('HR_MANAGER')) AS roles(r)
WHERE code = 'AI_DASHBOARD'
ON CONFLICT DO NOTHING;