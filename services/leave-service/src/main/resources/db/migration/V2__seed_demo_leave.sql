-- V2 — DEMO leave seed: balances for all 50 demo employees against the 5
-- default leave types, plus a realistic mix of leave requests (approved
-- past, currently active, pending HR review, and one rejected). DEMO DATA
-- — safe to drop. Gated on the demo employees existing so prod is untouched.

DO $$
DECLARE
    annual_id   UUID;
    sick_id     UUID;
    matern_id   UUID;
    unpaid_id   UUID;
    study_id    UUID;
    has_demo    BOOLEAN;
    yr          INT := EXTRACT(YEAR FROM CURRENT_DATE);
BEGIN
    -- Only seed when the demo company (V2 in employee-service) has been seeded.
    SELECT EXISTS (
        SELECT 1 FROM hrms_employee.employees
        WHERE id = '33333333-3333-3333-3333-000000000001'
    ) INTO has_demo;
    IF NOT has_demo THEN
        RAISE NOTICE 'Demo employees not present — skipping demo leave seed';
        RETURN;
    END IF;

    SELECT id INTO annual_id FROM leave_types WHERE code = 'ANNUAL'    AND is_deleted = FALSE;
    SELECT id INTO sick_id   FROM leave_types WHERE code = 'SICK'      AND is_deleted = FALSE;
    SELECT id INTO matern_id FROM leave_types WHERE code = 'MATERNITY' AND is_deleted = FALSE;
    SELECT id INTO unpaid_id FROM leave_types WHERE code = 'UNPAID'    AND is_deleted = FALSE;
    SELECT id INTO study_id  FROM leave_types WHERE code = 'STUDY'     AND is_deleted = FALSE;

    -- ── Balances: 5 types × 50 employees = 250 rows ──────────────────────
    -- entitled_days = leave_types.days_allowed. used_days is bumped further
    -- down for employees whose approved requests have already burned days.
    INSERT INTO leave_balances (employee_id, leave_type_id, year, entitled_days, created_by)
    SELECT
        e.id,
        t.id,
        yr,
        t.days_allowed,
        'seed'
    FROM hrms_employee.employees e
    CROSS JOIN leave_types t
    WHERE e.created_by = 'seed'
      AND t.is_deleted = FALSE
      AND NOT EXISTS (
          SELECT 1 FROM leave_balances b
          WHERE b.employee_id = e.id AND b.leave_type_id = t.id AND b.year = yr
      );

    -- ── Approved past leave (already returned to work) ───────────────────
    INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date,
        days_requested, reason, status, reviewed_by, reviewed_at, created_by) VALUES
        ('33333333-3333-3333-3333-000000000005', annual_id,
            CURRENT_DATE - INTERVAL '45 days', CURRENT_DATE - INTERVAL '39 days',
            7,  'Семейная поездка в Алмату',         'APPROVED',
            '33333333-3333-3333-3333-000000000001', CURRENT_DATE - INTERVAL '50 days', 'seed'),
        ('33333333-3333-3333-3333-000000000012', sick_id,
            CURRENT_DATE - INTERVAL '20 days', CURRENT_DATE - INTERVAL '17 days',
            4,  'Простуда, больничный лист',         'APPROVED',
            '33333333-3333-3333-3333-000000000009', CURRENT_DATE - INTERVAL '20 days', 'seed'),
        ('33333333-3333-3333-3333-000000000022', annual_id,
            CURRENT_DATE - INTERVAL '60 days', CURRENT_DATE - INTERVAL '51 days',
            10, 'Отпуск',                            'APPROVED',
            '33333333-3333-3333-3333-000000000012', CURRENT_DATE - INTERVAL '65 days', 'seed'),
        ('33333333-3333-3333-3333-000000000030', study_id,
            CURRENT_DATE - INTERVAL '14 days', CURRENT_DATE - INTERVAL '12 days',
            3,  'Сертификация AWS',                  'APPROVED',
            '33333333-3333-3333-3333-000000000016', CURRENT_DATE - INTERVAL '18 days', 'seed');

    -- ── Currently on leave today (drives the new ON_LEAVE badge) ─────────
    INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date,
        days_requested, reason, status, reviewed_by, reviewed_at, created_by) VALUES
        ('33333333-3333-3333-3333-000000000008', annual_id,
            CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '10 days',
            14, 'Двухнедельный отпуск',              'APPROVED',
            '33333333-3333-3333-3333-000000000001', CURRENT_DATE - INTERVAL '15 days', 'seed'),
        ('33333333-3333-3333-3333-000000000017', annual_id,
            CURRENT_DATE - INTERVAL '1 days', CURRENT_DATE + INTERVAL '5 days',
            7,  'Семейный отдых',                    'APPROVED',
            '33333333-3333-3333-3333-000000000009', CURRENT_DATE - INTERVAL '10 days', 'seed'),
        ('33333333-3333-3333-3333-000000000025', sick_id,
            CURRENT_DATE - INTERVAL '2 days', CURRENT_DATE + INTERVAL '1 days',
            4,  'Грипп',                             'APPROVED',
            '33333333-3333-3333-3333-000000000016', CURRENT_DATE - INTERVAL '2 days', 'seed'),
        ('33333333-3333-3333-3333-000000000038', unpaid_id,
            CURRENT_DATE,                       CURRENT_DATE + INTERVAL '7 days',
            8,  'Личные обстоятельства',             'APPROVED',
            '33333333-3333-3333-3333-000000000016', CURRENT_DATE - INTERVAL '5 days', 'seed');

    -- ── Pending HR approval (drives the approval queue) ──────────────────
    INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date,
        days_requested, reason, status, created_by) VALUES
        ('33333333-3333-3333-3333-000000000007', annual_id,
            CURRENT_DATE + INTERVAL '14 days', CURRENT_DATE + INTERVAL '23 days',
            10, 'Запланированный отпуск',     'PENDING', 'seed'),
        ('33333333-3333-3333-3333-000000000014', annual_id,
            CURRENT_DATE + INTERVAL '30 days', CURRENT_DATE + INTERVAL '36 days',
            7,  'Семейная поездка',           'PENDING', 'seed'),
        ('33333333-3333-3333-3333-000000000028', study_id,
            CURRENT_DATE + INTERVAL '10 days', CURRENT_DATE + INTERVAL '13 days',
            4,  'Курсы повышения квалификации','PENDING', 'seed'),
        ('33333333-3333-3333-3333-000000000041', annual_id,
            CURRENT_DATE + INTERVAL '21 days', CURRENT_DATE + INTERVAL '30 days',
            10, 'Зимний отпуск',              'PENDING', 'seed');

    -- ── One rejected (for variety in the requests view) ──────────────────
    INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date,
        days_requested, reason, status, reviewed_by, reviewed_at,
        review_comment, created_by) VALUES
        ('33333333-3333-3333-3333-000000000033', unpaid_id,
            CURRENT_DATE + INTERVAL '5 days', CURRENT_DATE + INTERVAL '12 days',
            8,  'Личные дела',
            'REJECTED', '33333333-3333-3333-3333-000000000012', CURRENT_DATE - INTERVAL '1 days',
            'Период пересекается с отчётным закрытием квартала. Перенесите.',
            'seed');

    -- ── Reflect burned days on the balance rows that have APPROVED leaves
    -- (today, past, and future-starting all count once approved).
    UPDATE leave_balances b
    SET used_days = used_days + r.total
    FROM (
        SELECT employee_id, leave_type_id, SUM(days_requested) AS total
        FROM leave_requests
        WHERE status = 'APPROVED' AND created_by = 'seed'
        GROUP BY employee_id, leave_type_id
    ) r
    WHERE b.employee_id = r.employee_id
      AND b.leave_type_id = r.leave_type_id
      AND b.year = yr
      AND b.is_deleted = FALSE;

    RAISE NOTICE 'Demo leave seed: 250 balances + 13 requests inserted';
END $$;