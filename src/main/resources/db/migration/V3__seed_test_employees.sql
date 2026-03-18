-- V3__seed_test_employees.sql
-- Test data for payroll module development and testing.
-- Creates 1 department, 1 position, and 5 employees with realistic KZ salaries.
-- Remove or replace this migration when employee module is fully implemented.

-- ── Department ──────────────────────────────────────────────────────────────
INSERT INTO departments (id, name, description, cost_center, is_deleted, created_at, updated_at, created_by)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Разработка',
    'Backend and frontend development team',
    'CC-001',
    false, NOW(), NOW(), 'system'
);

-- ── Position ────────────────────────────────────────────────────────────────
INSERT INTO positions (id, title, description, min_salary, max_salary, department_id, is_deleted, created_at, updated_at, created_by)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'Backend Developer',
    'Java Spring Boot developer',
    250000.00, 600000.00,
    'a0000000-0000-0000-0000-000000000001',
    false, NOW(), NOW(), 'system'
);

-- ── Employees ───────────────────────────────────────────────────────────────
-- 5 employees with different salary levels to test tax calculation variety

INSERT INTO employees (
    id, employee_number, first_name, last_name, middle_name,
    email, iin, phone, hire_date, status, employment_type,
    base_salary, department_id, position_id,
    is_resident, has_disability, is_pensioner, is_deleted,
    created_at, updated_at, created_by
) VALUES
-- Employee 1: Standard resident, 300 000 ₸
(
    'c0000000-0000-0000-0000-000000000001',
    'EMP-0001', 'Асель', 'Нурова', 'Бекова',
    'asel.nurova@test.kz', '950312300145', '+7 701 111 0001',
    '2023-01-15', 'ACTIVE', 'FULL_TIME',
    300000.00,
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    true, false, false, false,
    NOW(), NOW(), 'system'
),
-- Employee 2: Higher salary to test OPV cap boundary, 500 000 ₸
(
    'c0000000-0000-0000-0000-000000000002',
    'EMP-0002', 'Болат', 'Сейткали', 'Ержанович',
    'bolat.seitkali@test.kz', '880525400212', '+7 701 111 0002',
    '2022-06-01', 'ACTIVE', 'FULL_TIME',
    500000.00,
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    true, false, false, false,
    NOW(), NOW(), 'system'
),
-- Employee 3: Low salary — test IPN floor near zero, 90 000 ₸
(
    'c0000000-0000-0000-0000-000000000003',
    'EMP-0003', 'Дина', 'Ахметова', 'Маратовна',
    'dina.akhmetova@test.kz', '001010500323', '+7 701 111 0003',
    '2024-02-01', 'ACTIVE', 'FULL_TIME',
    90000.00,
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    true, false, false, false,
    NOW(), NOW(), 'system'
),
-- Employee 4: Non-resident — test flat 20% IPN rate, 350 000 ₸
(
    'c0000000-0000-0000-0000-000000000004',
    'EMP-0004', 'Alexey', 'Petrov', NULL,
    'alexey.petrov@test.kz', '820714600434', '+7 701 111 0004',
    '2023-09-01', 'ACTIVE', 'CONTRACT',
    350000.00,
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    false, false, false, false,    -- is_resident = false → IPN 20% flat
    NOW(), NOW(), 'system'
),
-- Employee 5: Pensioner — OPV should be 0, 280 000 ₸
(
    'c0000000-0000-0000-0000-000000000005',
    'EMP-0005', 'Нурлан', 'Жаксыбеков', 'Сабитович',
    'nurlan.zhaksybekov@test.kz', '560820700545', '+7 701 111 0005',
    '2021-03-10', 'ACTIVE', 'FULL_TIME',
    280000.00,
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    true, false, true, false,      -- is_pensioner = true → OPV = 0
    NOW(), NOW(), 'system'
);

-- ── Link admin user to first employee (so /my-payslips works for testing) ───
UPDATE users
SET employee_id = 'c0000000-0000-0000-0000-000000000001'
WHERE email = 'admin@hrms.kz';
