-- V3 — DEMO attendance seed: past 14 working days for all 50 demo
-- employees. Realistic mix: most PRESENT, ~8% LATE, ~4% ABSENT, plus
-- ON_LEAVE rows that mirror the leave-service V2 demo approvals so the
-- attendance grid and the leave page tell the same story. DEMO DATA —
-- gated on demo employees being present so prod won't pick it up.

DO $$
DECLARE
    has_demo BOOLEAN;
    insert_count INT;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM hrms_employee.employees
        WHERE id = '33333333-3333-3333-3333-000000000001'
    ) INTO has_demo;
    IF NOT has_demo THEN
        RAISE NOTICE 'Demo employees not present — skipping demo attendance seed';
        RETURN;
    END IF;

    -- For each (employee, weekday in the last 14 days) build a record.
    -- We use a deterministic pseudo-random pick from an md5 of the inputs so
    -- the same migration always produces the same pattern (idempotent if
    -- combined with the ON CONFLICT DO NOTHING below).
    --
    -- Status distribution per row:
    --   r in [0..86)   → PRESENT  (~86%)
    --   r in [86..94)  → LATE     (~8%)
    --   r in [94..98)  → ABSENT   (~4%)
    --   r in [98..100) → HALF_DAY (~2%)
    --
    -- ON_LEAVE rows are layered on top below to match approved leaves.
    WITH days AS (
        SELECT (CURRENT_DATE - (offset_days || ' days')::INTERVAL)::DATE AS work_date
        FROM generate_series(1, 14) AS offset_days
        WHERE EXTRACT(DOW FROM CURRENT_DATE - (offset_days || ' days')::INTERVAL) NOT IN (0, 6)
          -- Drop seeded KZ public holidays.
          AND (CURRENT_DATE - (offset_days || ' days')::INTERVAL)::DATE NOT IN (
              SELECT holiday_date FROM holidays WHERE is_deleted = FALSE
          )
    ),
    employees AS (
        SELECT id FROM hrms_employee.employees WHERE created_by = 'seed'
    ),
    base AS (
        SELECT
            e.id AS employee_id,
            d.work_date,
            -- Stable per-row pseudo-random 0..99
            ('x' || substr(md5(e.id::TEXT || d.work_date::TEXT), 1, 8))::BIT(32)::INT
              % 100 AS r,
            -- Stable per-row check-in minute offset 0..29 (for LATE rows, +20 to push past 09:15)
            ('x' || substr(md5(e.id::TEXT || d.work_date::TEXT || 'mins'), 1, 8))::BIT(32)::INT
              % 30 AS m
        FROM employees e CROSS JOIN days d
    )
    INSERT INTO attendance_records (employee_id, work_date, check_in, check_out,
        status, check_in_method, check_out_method, worked_minutes, overtime_minutes,
        created_by)
    SELECT
        b.employee_id,
        b.work_date,
        CASE
            WHEN r < 86 THEN (b.work_date + TIME '09:00' + (b.m || ' minutes')::INTERVAL)
            WHEN r < 94 THEN (b.work_date + TIME '09:15' + ((b.m + 20) || ' minutes')::INTERVAL)
            WHEN r < 98 THEN NULL                                                       -- ABSENT
            ELSE              (b.work_date + TIME '09:00' + (b.m || ' minutes')::INTERVAL)  -- HALF_DAY
        END,
        CASE
            WHEN r < 86 THEN (b.work_date + TIME '18:00' + ((b.m % 20) || ' minutes')::INTERVAL)
            WHEN r < 94 THEN (b.work_date + TIME '18:10' + ((b.m % 15) || ' minutes')::INTERVAL)
            WHEN r < 98 THEN NULL                                                       -- ABSENT
            ELSE              (b.work_date + TIME '13:30')                              -- HALF_DAY
        END,
        CASE
            WHEN r < 86 THEN 'PRESENT'
            WHEN r < 94 THEN 'LATE'
            WHEN r < 98 THEN 'ABSENT'
            ELSE              'HALF_DAY'
        END,
        CASE WHEN r < 98 THEN 'WEB' END,
        CASE WHEN r < 98 THEN 'WEB' END,
        CASE
            WHEN r < 86 THEN 480 + (b.m % 30)            -- 8h0m..8h29m
            WHEN r < 94 THEN 465 + (b.m % 20)            -- 7h45m..8h05m
            WHEN r < 98 THEN NULL                        -- ABSENT
            ELSE              240                        -- HALF_DAY 4h
        END,
        0,
        'seed'
    FROM base b
    -- Avoid colliding with rows the user may already have created via the UI.
    ON CONFLICT DO NOTHING;
    GET DIAGNOSTICS insert_count = ROW_COUNT;
    RAISE NOTICE 'Demo attendance seed: % regular working-day rows inserted', insert_count;

    -- ── ON_LEAVE rows for employees with approved leave intersecting the
    -- 14-day window. Mirrors the leave-service V2 demo data so attendance
    -- matches leave (the attendance LeaveEventsListener does this in prod,
    -- but seed data bypasses RabbitMQ).
    WITH window_days AS (
        SELECT (CURRENT_DATE - (offset_days || ' days')::INTERVAL)::DATE AS d
        FROM generate_series(1, 14) AS offset_days
        WHERE EXTRACT(DOW FROM CURRENT_DATE - (offset_days || ' days')::INTERVAL) NOT IN (0, 6)
    ),
    leaves AS (
        SELECT employee_id, leave_type_name, start_date, end_date
        FROM (VALUES
            ('33333333-3333-3333-3333-000000000008'::UUID, 'Annual Leave',
                (CURRENT_DATE - INTERVAL '3 days')::DATE, (CURRENT_DATE + INTERVAL '10 days')::DATE),
            ('33333333-3333-3333-3333-000000000017'::UUID, 'Annual Leave',
                (CURRENT_DATE - INTERVAL '1 days')::DATE, (CURRENT_DATE + INTERVAL '5 days')::DATE),
            ('33333333-3333-3333-3333-000000000025'::UUID, 'Sick Leave',
                (CURRENT_DATE - INTERVAL '2 days')::DATE, (CURRENT_DATE + INTERVAL '1 days')::DATE),
            ('33333333-3333-3333-3333-000000000012'::UUID, 'Sick Leave',
                (CURRENT_DATE - INTERVAL '20 days')::DATE, (CURRENT_DATE - INTERVAL '17 days')::DATE)
        ) AS x(employee_id, leave_type_name, start_date, end_date)
    )
    UPDATE attendance_records ar
    SET status = 'ON_LEAVE',
        check_in = NULL,
        check_out = NULL,
        check_in_method = NULL,
        check_out_method = NULL,
        worked_minutes = NULL,
        notes = 'ON_LEAVE via ' || l.leave_type_name
    FROM leaves l, window_days w
    WHERE ar.employee_id = l.employee_id
      AND ar.work_date = w.d
      AND w.d BETWEEN l.start_date AND l.end_date
      AND ar.is_deleted = FALSE;
    GET DIAGNOSTICS insert_count = ROW_COUNT;
    RAISE NOTICE 'Demo attendance seed: % rows flipped to ON_LEAVE', insert_count;
END $$;