-- V2 — Seed the 32 permission codes and map them to roles.

INSERT INTO permissions (code, description, module) VALUES
    -- System / admin
    ('SYSTEM_USERS',        'Manage users (CRUD)',                     'system'),
    ('SYSTEM_ROLES',        'Manage role-permission mappings',         'system'),
    ('SYSTEM_SETTINGS',     'Change system-wide settings',             'system'),
    ('SYSTEM_AUDIT',        'Read audit logs',                         'system'),
    -- Employee
    ('EMPLOYEE_READ',       'View employees',                          'employee'),
    ('EMPLOYEE_CREATE',     'Create new employees',                    'employee'),
    ('EMPLOYEE_UPDATE',     'Edit employee data',                      'employee'),
    ('EMPLOYEE_DELETE',     'Terminate / soft-delete employees',       'employee'),
    ('EMPLOYEE_DOCUMENTS',  'Manage employee documents',               'employee'),
    -- Department / position
    ('DEPT_MANAGE',         'Manage departments and positions',        'organization'),
    -- Attendance
    ('ATTENDANCE_READ',     'View attendance',                         'attendance'),
    ('ATTENDANCE_MANAGE',   'Edit / correct attendance',               'attendance'),
    ('ATTENDANCE_BIOMETRIC','Manage biometric devices',                'attendance'),
    -- Leave
    ('LEAVE_READ',          'View leave requests',                     'leave'),
    ('LEAVE_REQUEST',       'Submit own leave request',                'leave'),
    ('LEAVE_APPROVE_TEAM',  'Approve leave for direct reports',        'leave'),
    ('LEAVE_APPROVE_ALL',   'Approve leave for any employee',          'leave'),
    ('LEAVE_MANAGE_TYPES',  'Manage leave types and balances',         'leave'),
    -- Payroll
    ('PAYROLL_READ',        'View payslips (own)',                     'payroll'),
    ('PAYROLL_READ_ALL',    'View any payslip',                        'payroll'),
    ('PAYROLL_PROCESS',     'Run payroll calculation',                 'payroll'),
    ('PAYROLL_APPROVE',     'Approve calculated payroll',              'payroll'),
    ('PAYROLL_PAY',         'Mark payslips as paid / export banks',    'payroll'),
    ('PAYROLL_ADJUST',      'Add allowances / deductions',             'payroll'),
    -- Reports
    ('REPORT_OPERATIONAL',  'Operational reports (HR)',                'report'),
    ('REPORT_FINANCIAL',    'Financial reports (accounting)',          'report'),
    ('REPORT_EXECUTIVE',    'Executive dashboards',                    'report'),
    ('REPORT_FORM_200',     'Generate Form 200.00 (tax)',              'report'),
    -- AI / ML
    ('AI_ANOMALY',          'View AI anomaly flags',                   'ai'),
    ('AI_FORECAST',         'View forecasts and analytics',            'ai'),
    -- Integration
    ('INTEGRATION_1C',      'Run 1C sync',                             'integration'),
    ('INTEGRATION_BANK',    'Export bank files',                       'integration');

-- ────────────────────────────────────────────────────────────
-- Role → permission mapping
-- ────────────────────────────────────────────────────────────

-- SUPER_ADMIN — everything
INSERT INTO role_permissions (role, permission_id)
SELECT 'SUPER_ADMIN', id FROM permissions;

-- DIRECTOR — read-only analytics across the company
INSERT INTO role_permissions (role, permission_id)
SELECT 'DIRECTOR', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ','ATTENDANCE_READ','LEAVE_READ',
    'PAYROLL_READ_ALL','REPORT_OPERATIONAL','REPORT_FINANCIAL',
    'REPORT_EXECUTIVE','REPORT_FORM_200','AI_ANOMALY','AI_FORECAST','SYSTEM_AUDIT'
);

-- HR_MANAGER — full HR + payroll processing + approvals
INSERT INTO role_permissions (role, permission_id)
SELECT 'HR_MANAGER', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ','EMPLOYEE_CREATE','EMPLOYEE_UPDATE','EMPLOYEE_DELETE','EMPLOYEE_DOCUMENTS',
    'DEPT_MANAGE',
    'ATTENDANCE_READ','ATTENDANCE_MANAGE',
    'LEAVE_READ','LEAVE_APPROVE_ALL','LEAVE_MANAGE_TYPES',
    'PAYROLL_READ_ALL','PAYROLL_PROCESS','PAYROLL_APPROVE','PAYROLL_ADJUST',
    'REPORT_OPERATIONAL','REPORT_FINANCIAL','REPORT_FORM_200',
    'AI_ANOMALY'
);

-- HR_SPECIALIST — HR operations (no payroll)
INSERT INTO role_permissions (role, permission_id)
SELECT 'HR_SPECIALIST', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ','EMPLOYEE_CREATE','EMPLOYEE_UPDATE','EMPLOYEE_DOCUMENTS',
    'ATTENDANCE_READ','ATTENDANCE_MANAGE',
    'LEAVE_READ','LEAVE_APPROVE_TEAM','LEAVE_REQUEST',
    'REPORT_OPERATIONAL'
);

-- ACCOUNTANT — payroll finalization, financial reports
INSERT INTO role_permissions (role, permission_id)
SELECT 'ACCOUNTANT', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ',
    'PAYROLL_READ_ALL','PAYROLL_PAY',
    'REPORT_FINANCIAL','REPORT_FORM_200',
    'INTEGRATION_BANK','INTEGRATION_1C'
);

-- MANAGER — team management (approve team leave, view team attendance)
INSERT INTO role_permissions (role, permission_id)
SELECT 'MANAGER', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ',
    'ATTENDANCE_READ',
    'LEAVE_READ','LEAVE_REQUEST','LEAVE_APPROVE_TEAM',
    'PAYROLL_READ',
    'REPORT_OPERATIONAL'
);

-- TEAM_LEAD — narrower than MANAGER, same core actions
INSERT INTO role_permissions (role, permission_id)
SELECT 'TEAM_LEAD', id FROM permissions WHERE code IN (
    'EMPLOYEE_READ',
    'ATTENDANCE_READ',
    'LEAVE_READ','LEAVE_REQUEST','LEAVE_APPROVE_TEAM',
    'PAYROLL_READ'
);

-- EMPLOYEE — self-service
INSERT INTO role_permissions (role, permission_id)
SELECT 'EMPLOYEE', id FROM permissions WHERE code IN (
    'LEAVE_REQUEST','LEAVE_READ',
    'PAYROLL_READ',
    'ATTENDANCE_READ'
);