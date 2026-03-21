INSERT INTO users (id, first_name, last_name, email, password, role, enabled, account_non_locked)
VALUES (
    gen_random_uuid(),
    'Nursultan',
    'Admin',
    'admin@hrms.kz',
    '$2a$12$RkFhlu3/G2YHuMYZbtUxlOJ.yU4I5vJAaD1vT5zxBqbf7Yskc4ha2',
    'SUPER_ADMIN',
    true,
    true
);