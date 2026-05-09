-- V5 — Add the 8 permission codes the @PreAuthorize annotations actually use
-- but V2/V4 forgot to seed. Without these, even SUPER_ADMIN gets 403 on
-- payroll listing, attendance check-in, leave request submission, etc.,
-- because role_permissions can only reference IDs that exist in `permissions`.
--
-- Canonical names match docs/PERMISSIONS.md. Older synonymous codes
-- (PAYROLL_READ, LEAVE_REQUEST, LEAVE_MANAGE_TYPES, ATTENDANCE_READ) stay
-- in the table — removing them would orphan grants on existing role rows.
-- Future code should use the new codes; the old ones are effectively unused
-- by any controller and will be cleaned up in a later migration once we
-- can migrate any frontend that read them.

INSERT INTO permissions (code, description, module) VALUES
    ('ATTENDANCE_CHECKIN',   'Submit own check-in / check-out',          'attendance'),
    ('ATTENDANCE_VIEW_ALL',  'View company-wide attendance',             'attendance'),
    ('ATTENDANCE_VIEW_TEAM', 'View attendance for direct reports',       'attendance'),
    ('LEAVE_BALANCE_MANAGE', 'Adjust leave balances and manage types',   'leave'),
    ('LEAVE_REQUEST_OWN',    'Submit own leave request',                 'leave'),
    ('PAYROLL_VIEW',         'List payroll periods and view detail',     'payroll'),
    ('PAYSLIP_ADJUST',       'Per-employee payslip correction',          'payroll'),
    ('PAYSLIP_VIEW_OWN',     'View own payslips (self-service)',         'payroll')
ON CONFLICT (code) DO NOTHING;

-- SUPER_ADMIN — re-run the catch-all so the eight new codes land
INSERT INTO role_permissions (role, permission_id)
SELECT 'SUPER_ADMIN', id FROM permissions WHERE code IN (
    'ATTENDANCE_CHECKIN','ATTENDANCE_VIEW_ALL','ATTENDANCE_VIEW_TEAM',
    'LEAVE_BALANCE_MANAGE','LEAVE_REQUEST_OWN',
    'PAYROLL_VIEW','PAYSLIP_ADJUST','PAYSLIP_VIEW_OWN'
)
ON CONFLICT DO NOTHING;

-- ATTENDANCE_VIEW_ALL → admin/HR/director (full-company visibility)
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('DIRECTOR'),('HR_MANAGER'),('HR_SPECIALIST')) AS roles(r)
WHERE code = 'ATTENDANCE_VIEW_ALL'
ON CONFLICT DO NOTHING;

-- ATTENDANCE_VIEW_TEAM → managers / team leads
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('MANAGER'),('TEAM_LEAD')) AS roles(r)
WHERE code = 'ATTENDANCE_VIEW_TEAM'
ON CONFLICT DO NOTHING;

-- ATTENDANCE_CHECKIN, LEAVE_REQUEST_OWN, PAYSLIP_VIEW_OWN → every role
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES
    ('DIRECTOR'),('HR_MANAGER'),('HR_SPECIALIST'),('ACCOUNTANT'),
    ('MANAGER'),('TEAM_LEAD'),('EMPLOYEE')
) AS roles(r)
WHERE code IN ('ATTENDANCE_CHECKIN','LEAVE_REQUEST_OWN','PAYSLIP_VIEW_OWN')
ON CONFLICT DO NOTHING;

-- LEAVE_BALANCE_MANAGE → HR_MANAGER (the leave-balance authority)
INSERT INTO role_permissions (role, permission_id)
SELECT 'HR_MANAGER', id FROM permissions
WHERE code = 'LEAVE_BALANCE_MANAGE'
ON CONFLICT DO NOTHING;

-- PAYROLL_VIEW → admin/HR/accountant/director
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('DIRECTOR'),('HR_MANAGER'),('ACCOUNTANT')) AS roles(r)
WHERE code = 'PAYROLL_VIEW'
ON CONFLICT DO NOTHING;

-- PAYSLIP_ADJUST → admin/HR/accountant
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('HR_MANAGER'),('ACCOUNTANT')) AS roles(r)
WHERE code = 'PAYSLIP_ADJUST'
ON CONFLICT DO NOTHING;
