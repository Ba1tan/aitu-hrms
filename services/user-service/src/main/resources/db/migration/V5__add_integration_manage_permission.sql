-- V5 — Add the INTEGRATION_MANAGE permission (was documented in
-- docs/PERMISSIONS.md §2.8 but never seeded, so the integration-hub endpoints
-- fell back to SYSTEM_SETTINGS/PAYROLL_APPROVE and ACCOUNTANT couldn't trigger
-- a 1C sync / bank file as intended).
--
-- Granted to SUPER_ADMIN (full access) and ACCOUNTANT per §3 default matrix.
-- New permissions are NOT auto-granted to SUPER_ADMIN by the DB, so it is
-- listed explicitly here.

INSERT INTO permissions (code, description, module) VALUES
    ('INTEGRATION_MANAGE', 'Trigger 1C sync, generate bank file, view sync log', 'integration')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role, permission_id)
SELECT r, p.id
FROM permissions p
CROSS JOIN (VALUES ('SUPER_ADMIN'), ('ACCOUNTANT')) AS roles(r)
WHERE p.code = 'INTEGRATION_MANAGE'
ON CONFLICT DO NOTHING;
