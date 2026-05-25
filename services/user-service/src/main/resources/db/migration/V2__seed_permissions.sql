-- V2 — Canonical permission catalog + role→permission grants.
--
-- Source of truth: every code enforced by a @PreAuthorize in the services
-- today, plus SYSTEM_ROLES / SYSTEM_AUDIT for the pending admin UI endpoints.
-- Biometric, AI/ML and the older synonym codes (PAYROLL_READ, LEAVE_READ,
-- REPORT_FINANCIAL, …) were dropped along with those features — do not
-- re-introduce them without a matching @PreAuthorize.

INSERT INTO permissions (code, description, module) VALUES
    -- System / admin
    ('SYSTEM_USERS',         'Manage users (CRUD)',                       'system'),
    ('SYSTEM_ROLES',         'Manage role-permission mappings',           'system'),
    ('SYSTEM_SETTINGS',      'Change system-wide settings; 1C / bank',    'system'),
    ('SYSTEM_AUDIT',         'Read audit logs',                           'system'),
    -- Employee
    ('EMPLOYEE_READ',        'View employees / directory / export',       'employee'),
    ('EMPLOYEE_CREATE',      'Create new employees',                      'employee'),
    ('EMPLOYEE_UPDATE',      'Edit employee data and status',             'employee'),
    ('EMPLOYEE_DELETE',      'Terminate / soft-delete employees',         'employee'),
    ('EMPLOYEE_DOCUMENTS',   'Manage employee documents',                 'employee'),
    -- Organization
    ('DEPT_MANAGE',          'Manage departments and positions',          'organization'),
    -- Attendance
    ('ATTENDANCE_CHECKIN',   'Submit own check-in / check-out',           'attendance'),
    ('ATTENDANCE_MANAGE',    'Edit / correct attendance records',         'attendance'),
    ('ATTENDANCE_VIEW_ALL',  'View company-wide attendance',              'attendance'),
    ('ATTENDANCE_VIEW_TEAM', 'View attendance for direct reports',        'attendance'),
    -- Leave
    ('LEAVE_REQUEST_OWN',    'Submit own leave request',                  'leave'),
    ('LEAVE_APPROVE_TEAM',   'Approve leave for direct reports',          'leave'),
    ('LEAVE_APPROVE_ALL',    'Approve leave for any employee',            'leave'),
    ('LEAVE_BALANCE_MANAGE', 'Adjust leave balances and manage types',    'leave'),
    -- Payroll
    ('PAYROLL_VIEW',         'List payroll periods and view detail',      'payroll'),
    ('PAYROLL_READ_ALL',     'View any payslip',                          'payroll'),
    ('PAYROLL_PROCESS',      'Run payroll calculation',                   'payroll'),
    ('PAYROLL_APPROVE',      'Approve calculated payroll',                'payroll'),
    ('PAYROLL_PAY',          'Mark payslips paid / export bank files',    'payroll'),
    ('PAYROLL_ADJUST',       'Add allowances / deductions to a period',   'payroll'),
    ('PAYSLIP_VIEW_OWN',     'View own payslips (self-service)',          'payroll'),
    ('PAYSLIP_ADJUST',       'Per-employee payslip correction',           'payroll'),
    -- Reports
    ('REPORT_PAYROLL',       'Payroll reports, Form 200.00, salary breakdown', 'report'),
    ('REPORT_ATTENDANCE',    'Attendance monthly / summary reports',      'report'),
    ('REPORT_LEAVE',         'Leave balances report',                     'report'),
    ('REPORT_HR',            'Employee directory, headcount, turnover',   'report'),
    ('REPORT_EXECUTIVE',     'Executive all-in-one summary',              'report');

-- ────────────────────────────────────────────────────────────
-- Role → permission mapping (per docs/PERMISSIONS.md §1 role intents)
-- ────────────────────────────────────────────────────────────

-- SUPER_ADMIN — everything
INSERT INTO role_permissions (role, permission_id)
SELECT 'SUPER_ADMIN', id FROM permissions;

-- Self-service — granted to every (non-super) role
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES
    ('DIRECTOR'),('HR_MANAGER'),('HR_SPECIALIST'),('ACCOUNTANT'),
    ('MANAGER'),('TEAM_LEAD'),('EMPLOYEE')
) AS roles(r)
WHERE code IN ('ATTENDANCE_CHECKIN','LEAVE_REQUEST_OWN','PAYSLIP_VIEW_OWN')
ON CONFLICT DO NOTHING;

-- DIRECTOR — read-only company-wide analytics
INSERT INTO role_permissions (role, permission_id)
SELECT 'DIRECTOR', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ','ATTENDANCE_VIEW_ALL','PAYROLL_READ_ALL','PAYROLL_VIEW',
    'REPORT_EXECUTIVE','REPORT_PAYROLL','REPORT_HR','SYSTEM_AUDIT'
) ON CONFLICT DO NOTHING;

-- HR_MANAGER — full HR + payroll processing/approval + all reports
INSERT INTO role_permissions (role, permission_id)
SELECT 'HR_MANAGER', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ','EMPLOYEE_CREATE','EMPLOYEE_UPDATE','EMPLOYEE_DELETE','EMPLOYEE_DOCUMENTS',
    'DEPT_MANAGE',
    'ATTENDANCE_MANAGE','ATTENDANCE_VIEW_ALL',
    'LEAVE_APPROVE_ALL','LEAVE_BALANCE_MANAGE',
    'PAYROLL_VIEW','PAYROLL_READ_ALL','PAYROLL_PROCESS','PAYROLL_APPROVE','PAYROLL_ADJUST','PAYSLIP_ADJUST',
    'REPORT_PAYROLL','REPORT_ATTENDANCE','REPORT_LEAVE','REPORT_HR','REPORT_EXECUTIVE'
) ON CONFLICT DO NOTHING;

-- HR_SPECIALIST — HR operations, no payroll
INSERT INTO role_permissions (role, permission_id)
SELECT 'HR_SPECIALIST', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ','EMPLOYEE_CREATE','EMPLOYEE_UPDATE','EMPLOYEE_DOCUMENTS',
    'ATTENDANCE_MANAGE','ATTENDANCE_VIEW_ALL',
    'LEAVE_APPROVE_TEAM',
    'REPORT_ATTENDANCE','REPORT_LEAVE','REPORT_HR'
) ON CONFLICT DO NOTHING;

-- ACCOUNTANT — payroll finalization + payroll reports
INSERT INTO role_permissions (role, permission_id)
SELECT 'ACCOUNTANT', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ',
    'PAYROLL_VIEW','PAYROLL_READ_ALL','PAYROLL_PAY','PAYSLIP_ADJUST',
    'REPORT_PAYROLL'
) ON CONFLICT DO NOTHING;

-- MANAGER — department head (team visibility + team leave approval)
INSERT INTO role_permissions (role, permission_id)
SELECT 'MANAGER', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ','ATTENDANCE_VIEW_TEAM','LEAVE_APPROVE_TEAM'
) ON CONFLICT DO NOTHING;

-- TEAM_LEAD — sub-team lead (same core actions, narrower scope in service)
INSERT INTO role_permissions (role, permission_id)
SELECT 'TEAM_LEAD', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ','ATTENDANCE_VIEW_TEAM','LEAVE_APPROVE_TEAM'
) ON CONFLICT DO NOTHING;

-- EMPLOYEE — self-service only (granted above)