-- V3__seed_comprehensive_test_data.sql
-- All passwords are BCrypt hash of "password123"
-- Admin account from V2 remains: admin@hrms.kz / password

DELETE FROM payslips;
DELETE FROM payroll_periods;
DELETE FROM leave_balances;
DELETE FROM leave_requests;
DELETE FROM attendance_records;
DELETE FROM users WHERE email != 'admin@hrms.kz';
DELETE FROM employees;
DELETE FROM positions;
DELETE FROM departments;


-- DEPARTMENTS (6)

INSERT INTO departments (id, name, description, cost_center, is_deleted, created_at, updated_at, created_by) VALUES
('d1000000-0000-0000-0000-000000000001', 'Разработка',        'Backend, frontend, DevOps',            'CC-001', false, NOW(), NOW(), 'system'),
('d1000000-0000-0000-0000-000000000002', 'HR и кадры',        'Управление персоналом и рекрутинг',    'CC-002', false, NOW(), NOW(), 'system'),
('d1000000-0000-0000-0000-000000000003', 'Финансы',           'Бухгалтерия, расчёт зарплаты',         'CC-003', false, NOW(), NOW(), 'system'),
('d1000000-0000-0000-0000-000000000004', 'Продажи',           'B2B и B2C продажи',                    'CC-004', false, NOW(), NOW(), 'system'),
('d1000000-0000-0000-0000-000000000005', 'Операции',          'Административные и операционные задачи','CC-005', false, NOW(), NOW(), 'system'),
('d1000000-0000-0000-0000-000000000006', 'Маркетинг',         'Диджитал и бренд маркетинг',           'CC-006', false, NOW(), NOW(), 'system');


-- POSITIONS (12)

INSERT INTO positions (id, title, description, min_salary, max_salary, department_id, is_deleted, created_at, updated_at, created_by) VALUES
-- Разработка
('p1000000-0000-0000-0000-000000000001', 'Senior Backend Developer',  'Java/Spring Boot',         450000, 700000, 'd1000000-0000-0000-0000-000000000001', false, NOW(), NOW(), 'system'),
('p1000000-0000-0000-0000-000000000002', 'Backend Developer',         'Java/Spring Boot',         280000, 450000, 'd1000000-0000-0000-0000-000000000001', false, NOW(), NOW(), 'system'),
('p1000000-0000-0000-0000-000000000003', 'Frontend Developer',        'React/TypeScript',         250000, 420000, 'd1000000-0000-0000-0000-000000000001', false, NOW(), NOW(), 'system'),
('p1000000-0000-0000-0000-000000000004', 'DevOps Engineer',           'Docker/K8s/CI',            350000, 550000, 'd1000000-0000-0000-0000-000000000001', false, NOW(), NOW(), 'system'),
-- HR
('p1000000-0000-0000-0000-000000000005', 'HR Manager',                'Управление персоналом',    300000, 450000, 'd1000000-0000-0000-0000-000000000002', false, NOW(), NOW(), 'system'),
('p1000000-0000-0000-0000-000000000006', 'HR Specialist',             'Рекрутинг и кадры',        180000, 300000, 'd1000000-0000-0000-0000-000000000002', false, NOW(), NOW(), 'system'),
-- Финансы
('p1000000-0000-0000-0000-000000000007', 'Chief Accountant',          'Главный бухгалтер',        400000, 550000, 'd1000000-0000-0000-0000-000000000003', false, NOW(), NOW(), 'system'),
('p1000000-0000-0000-0000-000000000008', 'Accountant',                'Бухгалтер',                220000, 350000, 'd1000000-0000-0000-0000-000000000003', false, NOW(), NOW(), 'system'),
-- Продажи
('p1000000-0000-0000-0000-000000000009', 'Sales Manager',             'Менеджер по продажам',     250000, 500000, 'd1000000-0000-0000-0000-000000000004', false, NOW(), NOW(), 'system'),
('p1000000-0000-0000-0000-000000000010', 'Sales Representative',      'Специалист по продажам',   150000, 280000, 'd1000000-0000-0000-0000-000000000004', false, NOW(), NOW(), 'system'),
-- Операции
('p1000000-0000-0000-0000-000000000011', 'Operations Manager',        'Руководитель операций',    320000, 480000, 'd1000000-0000-0000-0000-000000000005', false, NOW(), NOW(), 'system'),
-- Маркетинг
('p1000000-0000-0000-0000-000000000012', 'Marketing Specialist',      'Диджитал маркетолог',      200000, 350000, 'd1000000-0000-0000-0000-000000000006', false, NOW(), NOW(), 'system');


-- EMPLOYEES (35)
-- Coverage: all departments, all statuses, various salary levels,
-- residents/non-resident, pensioner, disability, partial months


--РАЗРАБОТКА (10 employees)

INSERT INTO employees (id, employee_number, first_name, last_name, middle_name,
    email, iin, phone, hire_date, status, employment_type, base_salary,
    department_id, position_id, manager_id,
    is_resident, has_disability, is_pensioner, is_deleted, created_at, updated_at, created_by)
VALUES
-- E01: Tech Lead / MANAGER role
('e1000000-0000-0000-0000-000000000001', 'EMP-0001', 'Nursultan',  'Torekhanov', 'Bekovich',
 'n.torekhanov@hrms-demo.kz',     '910515300101', '+7 701 001 0001',
 '2020-03-01', 'ACTIVE', 'FULL_TIME', 580000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000001', NULL,
 true, false, false, false, NOW(), NOW(), 'system'),

-- E02: Senior dev
('e1000000-0000-0000-0000-000000000002', 'EMP-0002', 'Askar',      'Seralinov',  'Maratovich',
 'a.seralinov@hrms-demo.kz',      '930820400102', '+7 701 001 0002',
 '2021-06-15', 'ACTIVE', 'FULL_TIME', 520000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000001',
 'e1000000-0000-0000-0000-000000000001',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E03: Backend dev
('e1000000-0000-0000-0000-000000000003', 'EMP-0003', 'Nurbol',     'Sembayev',   'Aibatovich',
 'n.sembayev@hrms-demo.kz',       '960312500103', '+7 701 001 0003',
 '2022-01-10', 'ACTIVE', 'FULL_TIME', 350000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000002',
 'e1000000-0000-0000-0000-000000000001',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E04: Frontend dev
('e1000000-0000-0000-0000-000000000004', 'EMP-0004', 'Aizat',      'Bekova',     'Serikovna',
 'a.bekova@hrms-demo.kz',         '980605600104', '+7 701 001 0004',
 '2022-09-01', 'ACTIVE', 'FULL_TIME', 320000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000003',
 'e1000000-0000-0000-0000-000000000001',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E05: DevOps
('e1000000-0000-0000-0000-000000000005', 'EMP-0005', 'Daulet',     'Ahmetov',    'Zhanovich',
 'd.ahmetov@hrms-demo.kz',        '940210700105', '+7 701 001 0005',
 '2021-11-01', 'ACTIVE', 'FULL_TIME', 420000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000004',
 'e1000000-0000-0000-0000-000000000001',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E06: Junior backend — ON_LEAVE
('e1000000-0000-0000-0000-000000000006', 'EMP-0006', 'Samal',      'Nurlanова',  'Karimovna',
 's.nurlanova@hrms-demo.kz',      '000101800106', '+7 701 001 0006',
 '2023-03-15', 'ON_LEAVE', 'FULL_TIME', 280000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000002',
 'e1000000-0000-0000-0000-000000000001',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E07: Non-resident contractor — tests 20% IPN flat rate
('e1000000-0000-0000-0000-000000000007', 'EMP-0007', 'Alexey',     'Petrov',     NULL,
 'a.petrov@hrms-demo.kz',         '820714600107', '+7 701 001 0007',
 '2023-08-01', 'ACTIVE', 'CONTRACT', 450000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000001',
 'e1000000-0000-0000-0000-000000000001',
 false, false, false, false, NOW(), NOW(), 'system'),

-- E08: Frontend dev — PROBATION
('e1000000-0000-0000-0000-000000000008', 'EMP-0008', 'Madina',     'Seitkali',   'Bolатовна',
 'm.seitkali@hrms-demo.kz',       '010203900108', '+7 701 001 0008',
 '2026-01-15', 'PROBATION', 'FULL_TIME', 250000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000003',
 'e1000000-0000-0000-0000-000000000001',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E09: Backend dev — disability (882 MRP deduction test)
('e1000000-0000-0000-0000-000000000009', 'EMP-0009', 'Zhandos',    'Mukanov',    'Serikovich',
 'z.mukanov@hrms-demo.kz',        '950820100109', '+7 701 001 0009',
 '2022-04-01', 'ACTIVE', 'FULL_TIME', 300000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000002',
 'e1000000-0000-0000-0000-000000000001',
 true, true, false, false, NOW(), NOW(), 'system'),

-- E10: Senior dev — TERMINATED
('e1000000-0000-0000-0000-000000000010', 'EMP-0010', 'Ruslan',     'Dzhaksybekov','Nurlanovich',
 'r.dzhaksybekov@hrms-demo.kz',   '880910200110', '+7 701 001 0010',
 '2019-05-01', 'TERMINATED', 'FULL_TIME', 500000.00,
 'd1000000-0000-0000-0000-000000000001', 'p1000000-0000-0000-0000-000000000001',
 'e1000000-0000-0000-0000-000000000001',
 true, false, false, false, NOW(), NOW(), 'system'),

-- HR И КАДРЫ (5 employees)

-- E11: HR Manager — has HR_MANAGER user role
('e1000000-0000-0000-0000-000000000011', 'EMP-0011', 'Ainur',      'Zhaksylykova','Bekova',
 'a.zhaksylykova@hrms-demo.kz',   '870325300111', '+7 701 001 0011',
 '2019-09-01', 'ACTIVE', 'FULL_TIME', 420000.00,
 'd1000000-0000-0000-0000-000000000002', 'p1000000-0000-0000-0000-000000000005', NULL,
 true, false, false, false, NOW(), NOW(), 'system'),

-- E12: HR Specialist
('e1000000-0000-0000-0000-000000000012', 'EMP-0012', 'Gulnara',    'Omarova',    'Askarovna',
 'g.omarova@hrms-demo.kz',        '921105400112', '+7 701 001 0012',
 '2021-02-01', 'ACTIVE', 'FULL_TIME', 240000.00,
 'd1000000-0000-0000-0000-000000000002', 'p1000000-0000-0000-0000-000000000006',
 'e1000000-0000-0000-0000-000000000011',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E13: HR Specialist
('e1000000-0000-0000-0000-000000000013', 'EMP-0013', 'Dinara',     'Sattarova',  'Маратovна',
 'd.sattarova@hrms-demo.kz',      '940607500113', '+7 701 001 0013',
 '2022-07-01', 'ACTIVE', 'FULL_TIME', 220000.00,
 'd1000000-0000-0000-0000-000000000002', 'p1000000-0000-0000-0000-000000000006',
 'e1000000-0000-0000-0000-000000000011',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E14: HR Specialist — PART_TIME
('e1000000-0000-0000-0000-000000000014', 'EMP-0014', 'Aliya',      'Bekmuratova', 'Seitkaliевна',
 'a.bekmuratova@hrms-demo.kz',    '990815600114', '+7 701 001 0014',
 '2023-11-01', 'ACTIVE', 'PART_TIME', 150000.00,
 'd1000000-0000-0000-0000-000000000002', 'p1000000-0000-0000-0000-000000000006',
 'e1000000-0000-0000-0000-000000000011',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E15: HR Specialist — pensioner (OPV=0 test)
('e1000000-0000-0000-0000-000000000015', 'EMP-0015', 'Roza',       'Abdullayeva', 'Karimovna',
 'r.abdullayeva@hrms-demo.kz',    '581220700115', '+7 701 001 0015',
 '2020-01-15', 'ACTIVE', 'FULL_TIME', 200000.00,
 'd1000000-0000-0000-0000-000000000002', 'p1000000-0000-0000-0000-000000000006',
 'e1000000-0000-0000-0000-000000000011',
 true, false, true, false, NOW(), NOW(), 'system'),

-- ── ФИНАНСЫ (5 employees) ──────────────────────────────────────────────────

-- E16: Chief Accountant — has ACCOUNTANT user role
('e1000000-0000-0000-0000-000000000016', 'EMP-0016', 'Zulfiya',    'Yessenova',  'Аскаровна',
 'z.yessenova@hrms-demo.kz',      '830412800116', '+7 701 001 0016',
 '2018-06-01', 'ACTIVE', 'FULL_TIME', 480000.00,
 'd1000000-0000-0000-0000-000000000003', 'p1000000-0000-0000-0000-000000000007', NULL,
 true, false, false, false, NOW(), NOW(), 'system'),

-- E17: Accountant
('e1000000-0000-0000-0000-000000000017', 'EMP-0017', 'Maral',      'Duysenova',  'Nurлановна',
 'm.duysenova@hrms-demo.kz',      '900315900117', '+7 701 001 0017',
 '2020-09-01', 'ACTIVE', 'FULL_TIME', 280000.00,
 'd1000000-0000-0000-0000-000000000003', 'p1000000-0000-0000-0000-000000000008',
 'e1000000-0000-0000-0000-000000000016',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E18: Accountant — high salary to test OPV cap edge (50×MZP = 4250000)
('e1000000-0000-0000-0000-000000000018', 'EMP-0018', 'Bauyrzhan',  'Seitkali',   'Nурланович',
 'b.seitkali@hrms-demo.kz',       '780520100118', '+7 701 001 0018',
 '2015-03-01', 'ACTIVE', 'FULL_TIME', 550000.00,
 'd1000000-0000-0000-0000-000000000003', 'p1000000-0000-0000-0000-000000000008',
 'e1000000-0000-0000-0000-000000000016',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E19: Accountant — ON_LEAVE
('e1000000-0000-0000-0000-000000000019', 'EMP-0019', 'Saltanat',   'Bekova',     'Жандосовна',
 's.bekova@hrms-demo.kz',         '950715200119', '+7 701 001 0019',
 '2021-04-15', 'ON_LEAVE', 'FULL_TIME', 260000.00,
 'd1000000-0000-0000-0000-000000000003', 'p1000000-0000-0000-0000-000000000008',
 'e1000000-0000-0000-0000-000000000016',
 true, false, false, false, NOW(), NOW(), 'system'),

-- E20: Accountant — low salary near IPN floor test
('e1000000-0000-0000-0000-000000000020', 'EMP-0020', 'Arman',      'Nurmagambetov','Серікович',
 'a.nurmagambetov@hrms-demo.kz',  '010910300120', '+7 701 001 0020',
 '2024-06-01', 'PROBATION', 'FULL_TIME', 90000.00,
 'd1000000-0000-0000-0000-000000000003', 'p1000000-0000-0000-0000-000000000008',
 'e1000000-0000-0000-0000-000000000016',
 true, false, false, false, NOW(), NOW(), 'system'),

--ПРОДАЖИ (7 employees)

-- E21: Sales Manager — has MANAGER user role
('e1000000-0000-0000-0000-000000000021', 'EMP-0021', 'Berik',      'Seilov',     'Маратович',
 'b.seilov@hrms-demo.kz',         '850630400121', '+7 701 001 0021',
 '2019-01-15', 'ACTIVE', 'FULL_TIME', 400000.00,
 'd1000000-0000-0000-0000-000000000004', 'p1000000-0000-0000-0000-000000000009', NULL,
 true, false, false, false, NOW(), NOW(), 'system'),

-- E22-E27: Sales reps
('e1000000-0000-0000-0000-000000000022', 'EMP-0022', 'Asel',       'Nurova',     'Бекова',
 'asel.nurova@hrms-demo.kz',      '950312300122', '+7 701 001 0022',
 '2021-08-01', 'ACTIVE', 'FULL_TIME', 200000.00,
 'd1000000-0000-0000-0000-000000000004', 'p1000000-0000-0000-0000-000000000010',
 'e1000000-0000-0000-0000-000000000021',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000023', 'EMP-0023', 'Timur',      'Akhanov',    'Серікович',
 't.akhanov@hrms-demo.kz',        '970420500123', '+7 701 001 0023',
 '2022-03-01', 'ACTIVE', 'FULL_TIME', 185000.00,
 'd1000000-0000-0000-0000-000000000004', 'p1000000-0000-0000-0000-000000000010',
 'e1000000-0000-0000-0000-000000000021',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000024', 'EMP-0024', 'Symbat',     'Karimova',   'Нурлановна',
 's.karimova@hrms-demo.kz',       '000825600124', '+7 701 001 0024',
 '2023-01-09', 'ACTIVE', 'FULL_TIME', 175000.00,
 'd1000000-0000-0000-0000-000000000004', 'p1000000-0000-0000-0000-000000000010',
 'e1000000-0000-0000-0000-000000000021',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000025', 'EMP-0025', 'Aidana',     'Smagulova',  'Маратовна',
 'a.smagulova@hrms-demo.kz',      '980112700125', '+7 701 001 0025',
 '2023-05-15', 'ACTIVE', 'FULL_TIME', 165000.00,
 'd1000000-0000-0000-0000-000000000004', 'p1000000-0000-0000-0000-000000000010',
 'e1000000-0000-0000-0000-000000000021',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000026', 'EMP-0026', 'Daniyar',    'Zhumabekov', 'Аскаров',
 'd.zhumabekov@hrms-demo.kz',     '940930800126', '+7 701 001 0026',
 '2022-10-01', 'ACTIVE', 'CONTRACT', 220000.00,
 'd1000000-0000-0000-0000-000000000004', 'p1000000-0000-0000-0000-000000000010',
 'e1000000-0000-0000-0000-000000000021',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000027', 'EMP-0027', 'Nazgul',     'Tleubayeva', 'Бекова',
 'n.tleubayeva@hrms-demo.kz',     '011215900127', '+7 701 001 0027',
 '2025-11-01', 'PROBATION', 'FULL_TIME', 150000.00,
 'd1000000-0000-0000-0000-000000000004', 'p1000000-0000-0000-0000-000000000010',
 'e1000000-0000-0000-0000-000000000021',
 true, false, false, false, NOW(), NOW(), 'system'),

-- ОПЕРАЦИИ (4 employees)

-- E28: Operations Manager — has MANAGER user role
('e1000000-0000-0000-0000-000000000028', 'EMP-0028', 'Serik',      'Bekmuratov', 'Жандосович',
 's.bekmuratov@hrms-demo.kz',     '800715100128', '+7 701 001 0028',
 '2018-11-01', 'ACTIVE', 'FULL_TIME', 380000.00,
 'd1000000-0000-0000-0000-000000000005', 'p1000000-0000-0000-0000-000000000011', NULL,
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000029', 'EMP-0029', 'Tolkyn',     'Abenova',    'Серікова',
 't.abenova@hrms-demo.kz',        '921010200129', '+7 701 001 0029',
 '2021-03-01', 'ACTIVE', 'FULL_TIME', 220000.00,
 'd1000000-0000-0000-0000-000000000005', 'p1000000-0000-0000-0000-000000000011',
 'e1000000-0000-0000-0000-000000000028',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000030', 'EMP-0030', 'Yerlan',     'Seitkali',   'Болатович',
 'y.seitkali@hrms-demo.kz',       '880320300130', '+7 701 001 0030',
 '2020-08-15', 'ACTIVE', 'FULL_TIME', 210000.00,
 'd1000000-0000-0000-0000-000000000005', 'p1000000-0000-0000-0000-000000000011',
 'e1000000-0000-0000-0000-000000000028',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000031', 'EMP-0031', 'Akbota',     'Tursunova',  'Маратовна',
 'a.tursunova@hrms-demo.kz',      '960505400131', '+7 701 001 0031',
 '2023-02-01', 'ACTIVE', 'PART_TIME', 130000.00,
 'd1000000-0000-0000-0000-000000000005', 'p1000000-0000-0000-0000-000000000011',
 'e1000000-0000-0000-0000-000000000028',
 true, false, false, false, NOW(), NOW(), 'system'),

-- ── МАРКЕТИНГ (4 employees) ────────────────────────────────────────────────

('e1000000-0000-0000-0000-000000000032', 'EMP-0032', 'Kamilla',    'Bekova',     'Нурлановна',
 'k.bekova@hrms-demo.kz',         '940210500132', '+7 701 001 0032',
 '2021-07-01', 'ACTIVE', 'FULL_TIME', 280000.00,
 'd1000000-0000-0000-0000-000000000006', 'p1000000-0000-0000-0000-000000000012', NULL,
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000033', 'EMP-0033', 'Aruzhan',    'Smagulova',  'Бекова',
 'a.smagulova2@hrms-demo.kz',     '990318600133', '+7 701 001 0033',
 '2022-05-15', 'ACTIVE', 'FULL_TIME', 230000.00,
 'd1000000-0000-0000-0000-000000000006', 'p1000000-0000-0000-0000-000000000012',
 'e1000000-0000-0000-0000-000000000032',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000034', 'EMP-0034', 'Damir',      'Nurmagambetov','Серікович',
 'd.nurmagambetov@hrms-demo.kz',  '010420700134', '+7 701 001 0034',
 '2024-01-15', 'ACTIVE', 'FULL_TIME', 210000.00,
 'd1000000-0000-0000-0000-000000000006', 'p1000000-0000-0000-0000-000000000012',
 'e1000000-0000-0000-0000-000000000032',
 true, false, false, false, NOW(), NOW(), 'system'),

('e1000000-0000-0000-0000-000000000035', 'EMP-0035', 'Sholpan',    'Abenova',    'Маратовна',
 's.abenova@hrms-demo.kz',        '970715800135', '+7 701 001 0035',
 '2023-09-01', 'ACTIVE', 'INTERN', 100000.00,
 'd1000000-0000-0000-0000-000000000006', 'p1000000-0000-0000-0000-000000000012',
 'e1000000-0000-0000-0000-000000000032',
 true, false, false, false, NOW(), NOW(), 'system');

--
-- UPDATE DEPARTMENT MANAGERS
--
UPDATE departments SET manager_id = 'e1000000-0000-0000-0000-000000000001'
  WHERE id = 'd1000000-0000-0000-0000-000000000001';
UPDATE departments SET manager_id = 'e1000000-0000-0000-0000-000000000011'
  WHERE id = 'd1000000-0000-0000-0000-000000000002';
UPDATE departments SET manager_id = 'e1000000-0000-0000-0000-000000000016'
  WHERE id = 'd1000000-0000-0000-0000-000000000003';
UPDATE departments SET manager_id = 'e1000000-0000-0000-0000-000000000021'
  WHERE id = 'd1000000-0000-0000-0000-000000000004';
UPDATE departments SET manager_id = 'e1000000-0000-0000-0000-000000000028'
  WHERE id = 'd1000000-0000-0000-0000-000000000005';
UPDATE departments SET manager_id = 'e1000000-0000-0000-0000-000000000032'
  WHERE id = 'd1000000-0000-0000-0000-000000000006';


-- USER ACCOUNTS
-- All passwords = BCrypt of "password123"
-- Roles assigned to cover all RBAC scenarios

-- SUPER_ADMIN (update admin to link to no specific employee)
UPDATE users SET employee_id = NULL WHERE email = 'admin@hrms.kz';

-- HR_MANAGER — Ainur Zhaksylykova (E11)
INSERT INTO users (id, email, password, first_name, last_name, role, employee_id, enabled, is_deleted, created_at, updated_at, created_by)
VALUES
('u2000000-0000-0000-0000-000000000001',
 'a.zhaksylykova@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Ainur', 'Zhaksylykova', 'HR_MANAGER',
 'e1000000-0000-0000-0000-000000000011',
 true, false, NOW(), NOW(), 'system'),

-- ACCOUNTANT — Zulfiya Yessenova (E16)
('u2000000-0000-0000-0000-000000000002',
 'z.yessenova@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Zulfiya', 'Yessenova', 'ACCOUNTANT',
 'e1000000-0000-0000-0000-000000000016',
 true, false, NOW(), NOW(), 'system'),

-- MANAGER — Nursultan Torekhanov (E01, Tech Lead)
('u2000000-0000-0000-0000-000000000003',
 'n.torekhanov@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Nursultan', 'Torekhanov', 'MANAGER',
 'e1000000-0000-0000-0000-000000000001',
 true, false, NOW(), NOW(), 'system'),

-- MANAGER — Berik Seilov (E21, Sales Manager)
('u2000000-0000-0000-0000-000000000004',
 'b.seilov@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Berik', 'Seilov', 'MANAGER',
 'e1000000-0000-0000-0000-000000000021',
 true, false, NOW(), NOW(), 'system'),

-- MANAGER — Serik Bekmuratov (E28, Operations Manager)
('u2000000-0000-0000-0000-000000000005',
 's.bekmuratov@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Serik', 'Bekmuratov', 'MANAGER',
 'e1000000-0000-0000-0000-000000000028',
 true, false, NOW(), NOW(), 'system'),

-- EMPLOYEE accounts (representative sample — one per department)
-- Askar Seralinov — dev team
('u2000000-0000-0000-0000-000000000006',
 'a.seralinov@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Askar', 'Seralinov', 'EMPLOYEE',
 'e1000000-0000-0000-0000-000000000002',
 true, false, NOW(), NOW(), 'system'),

-- Nurbol Sembayev — frontend
('u2000000-0000-0000-0000-000000000007',
 'n.sembayev@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Nurbol', 'Sembayev', 'EMPLOYEE',
 'e1000000-0000-0000-0000-000000000003',
 true, false, NOW(), NOW(), 'system'),

-- Asel Nurova — sales
('u2000000-0000-0000-0000-000000000008',
 'asel.nurova@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Asel', 'Nurova', 'EMPLOYEE',
 'e1000000-0000-0000-0000-000000000022',
 true, false, NOW(), NOW(), 'system'),

-- Maral Duysenova — finance
('u2000000-0000-0000-0000-000000000009',
 'm.duysenova@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Maral', 'Duysenova', 'EMPLOYEE',
 'e1000000-0000-0000-0000-000000000017',
 true, false, NOW(), NOW(), 'system'),

-- Kamilla Bekova — marketing
('u2000000-0000-0000-0000-000000000010',
 'k.bekova@hrms-demo.kz',
 '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
 'Kamilla', 'Bekova', 'EMPLOYEE',
 'e1000000-0000-0000-0000-000000000032',
 true, false, NOW(), NOW(), 'system');
