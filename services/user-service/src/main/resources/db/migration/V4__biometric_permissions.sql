-- V4 — Biometric-related permissions + scoped employee view permissions.
--
-- EMPLOYEE_VIEW_OWN / EMPLOYEE_VIEW_ALL let services gate endpoints that
-- should be accessible to the employee themselves OR to HR admins with full
-- visibility (e.g. GET /v1/employees/{id}/biometric/status per the
-- employee-service CLAUDE.md spec).

INSERT INTO permissions (code, description, module) VALUES
    ('EMPLOYEE_VIEW_OWN',   'View own employee record (self-service)',    'employee'),
    ('EMPLOYEE_VIEW_ALL',   'View any employee record',                   'employee'),
    ('EMPLOYEE_BIOMETRIC',  'Enroll / remove biometric (face) data',      'employee');

-- SUPER_ADMIN already has every permission via the V2 catch-all insert —
-- re-run the catch-all so the three new codes are granted.
INSERT INTO role_permissions (role, permission_id)
SELECT 'SUPER_ADMIN', id FROM permissions WHERE code IN (
    'EMPLOYEE_VIEW_OWN','EMPLOYEE_VIEW_ALL','EMPLOYEE_BIOMETRIC'
)
ON CONFLICT DO NOTHING;

-- EMPLOYEE_VIEW_ALL → roles that already have full-company visibility
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('DIRECTOR'),('HR_MANAGER'),('HR_SPECIALIST')) AS roles(r)
WHERE code = 'EMPLOYEE_VIEW_ALL'
ON CONFLICT DO NOTHING;

-- EMPLOYEE_VIEW_OWN → everyone (self-service is universal)
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES
    ('DIRECTOR'),('HR_MANAGER'),('HR_SPECIALIST'),('ACCOUNTANT'),
    ('MANAGER'),('TEAM_LEAD'),('EMPLOYEE')
) AS roles(r)
WHERE code = 'EMPLOYEE_VIEW_OWN'
ON CONFLICT DO NOTHING;

-- EMPLOYEE_BIOMETRIC → HR roles that manage employee data
INSERT INTO role_permissions (role, permission_id)
SELECT r, id FROM permissions
CROSS JOIN (VALUES ('HR_MANAGER'),('HR_SPECIALIST')) AS roles(r)
WHERE code = 'EMPLOYEE_BIOMETRIC'
ON CONFLICT DO NOTHING;