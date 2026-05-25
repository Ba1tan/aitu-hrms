-- V3 — Seed the default SUPER_ADMIN account.
-- Email: admin@hrms.kz — shares the common demo BCrypt(cost 12) password hash.
-- Change immediately in production.

INSERT INTO users (id, first_name, last_name, email, password, role,
                   enabled, account_non_locked, require_password_change,
                   failed_login_count, is_deleted)
VALUES (
           gen_random_uuid(),
           'Nursultan', 'Admin', 'admin@hrms.kz',
           '$2a$12$Mop8tWI8HeEnTTX7/sxDd.0Jdz9f0zcd9VtB4wxxa8S9TsU7z3uAa',
           'SUPER_ADMIN',
           TRUE, TRUE, FALSE,
           0, FALSE
       )
    ON CONFLICT (email) DO NOTHING;