-- V3 — Seed a default SUPER_ADMIN account for bootstrapping.
-- Email:    admin@hrms.kz
-- Password: password123   (BCrypt cost 12)
-- Change immediately in production.

INSERT INTO users (id, first_name, last_name, email, password, role,
                   enabled, account_non_locked, require_password_change,
                   failed_login_count, is_deleted)
VALUES (
    gen_random_uuid(),
    'Nursultan', 'Admin', 'admin@hrms.kz',
    '$2a$12$xZktUBw.ab92RvUBeoUVgeocdbMbJQJvCnVkgsKiy18vyUoeDsYmW',
    'SUPER_ADMIN',
    TRUE, TRUE, TRUE,
    0, FALSE
)
ON CONFLICT (email) DO NOTHING;