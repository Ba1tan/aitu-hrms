INSERT INTO users (id, first_name, last_name, email, password, role, enabled, account_non_locked)
VALUES (
    gen_random_uuid(),
    'Nursultan',
    'Admin',
    'admin@hrms.kz',
    '$2a$12$xZktUBw.ab92RvUBeoUVgeocdbMbJQJvCnVkgsKiy18vyUoeDsYmW',
    'SUPER_ADMIN',
    true,
    true
);