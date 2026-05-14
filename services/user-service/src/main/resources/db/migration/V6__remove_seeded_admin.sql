-- V6 — Remove the seeded admin account created by V3.
--
-- The new public bootstrap flow (POST /v1/auth/bootstrap) replaces the
-- fixed `admin@hrms.kz / password123` seed: on a fresh tenant the very
-- first visitor to the landing page registers the SUPER_ADMIN themselves.
-- A pre-baked credential in source control is a footgun for real
-- deployments — anyone who reads CLAUDE.md owns the tenant.
--
-- We can't just delete V3 (Flyway checksum failure for already-migrated
-- databases), so this migration deletes the seeded row when it's still
-- in its pristine state — same email, default name, never logged in.
-- That guard means dev DBs where the admin has actually been used keep
-- their account intact and you can wipe the row manually if you need to
-- re-bootstrap.

DELETE FROM users
 WHERE email = 'admin@hrms.kz'
   AND role = 'SUPER_ADMIN'
   AND last_login_at IS NULL
   AND first_name = 'Nursultan'
   AND last_name = 'Admin';
