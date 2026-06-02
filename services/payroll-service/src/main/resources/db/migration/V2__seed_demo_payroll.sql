-- V2 — DEMO payroll seed: one APPROVED past period with payslips for all
-- 50 demo employees, plus an empty DRAFT period for the current month so
-- the "Generate payslips" button has a target. DEMO DATA — gated.
--
-- Tax math mirrors KazakhstanPayrollCalculator for 2026:
--   МРП=4325  МЗП=85000
--   earned   = gross  (assume full-month worked for demo)
--   OPV      = earned × 10%
--   ВОСМС    = earned ×  2%
--   deduction = 30 × МРП = 129,750 (resident, no disability)
--   taxable  = max(0, earned − OPV − ВОСМС − deduction)
--   IPN      = taxable × 10%
--   net      = earned − OPV − ВОСМС − IPN
--   SO       = (earned − OPV) × 5%
--   SN       = earned × 6%
--   ОПВР     = earned × 3.5%

DO $$
DECLARE
    has_demo  BOOLEAN;
    last_year  INT;
    last_month INT;
    last_period_id UUID;
    cur_period_id  UUID;
    last_start DATE;
    last_end   DATE;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM hrms_employee.employees
        WHERE id = '33333333-3333-3333-3333-000000000001'
    ) INTO has_demo;
    IF NOT has_demo THEN
        RAISE NOTICE 'Demo employees not present — skipping demo payroll seed';
        RETURN;
    END IF;

    last_year  := EXTRACT(YEAR  FROM (CURRENT_DATE - INTERVAL '1 month'));
    last_month := EXTRACT(MONTH FROM (CURRENT_DATE - INTERVAL '1 month'));
    last_start := MAKE_DATE(last_year, last_month, 1);
    last_end   := (last_start + INTERVAL '1 month - 1 day')::DATE;

    -- ── Past period: APPROVED, with payslips ─────────────────────────────
    INSERT INTO payroll_periods (year, month, start_date, end_date, working_days, status,
        processed_by, processed_at, approved_by, approved_at, created_by)
    VALUES (
        last_year, last_month, last_start, last_end,
        -- Working days = count of Mon–Fri in the month (KZ public holidays
        -- not strictly excluded here; the calculator uses the actual count
        -- when payslips are generated via the service path).
        (SELECT COUNT(*)::INT FROM generate_series(last_start, last_end, INTERVAL '1 day') d
         WHERE EXTRACT(DOW FROM d) NOT IN (0, 6)),
        'APPROVED',
        '33333333-3333-3333-3333-000000000001',  -- processed by Nursultan
        last_end + INTERVAL '2 days',
        '33333333-3333-3333-3333-000000000001',  -- approved by Nursultan
        last_end + INTERVAL '3 days',
        'seed'
    )
    ON CONFLICT DO NOTHING
    RETURNING id INTO last_period_id;

    IF last_period_id IS NULL THEN
        SELECT id INTO last_period_id FROM payroll_periods
        WHERE year = last_year AND month = last_month AND is_deleted = FALSE;
    END IF;

    -- Bulk-insert payslips for every demo employee using the tax math above.
    INSERT INTO payslips (period_id, employee_id, employee_iin, employee_name,
        employee_number, department_name, position_title,
        worked_days, total_working_days,
        gross_salary, earned_salary, allowances, other_deductions,
        opv_amount, vosms_amount, oopv_amount, taxable_income, ipn_amount,
        total_deductions, net_salary,
        so_amount, sn_amount, opvr_amount,
        mrp_used, is_resident, has_disability,
        status, created_by)
    SELECT
        last_period_id,
        e.id,
        e.iin,
        TRIM(BOTH FROM e.first_name || ' ' || e.last_name),
        e.employee_number,
        d.name,
        p.title,
        wd.working_days, wd.working_days,
        e.base_salary,                                          -- gross
        e.base_salary,                                          -- earned (full month)
        0, 0,
        ROUND(e.base_salary * 0.10, 2),                         -- OPV
        ROUND(e.base_salary * 0.02, 2),                         -- ВОСМС
        0,                                                      -- OOPV (not used here)
        GREATEST(0, ROUND(e.base_salary
            - e.base_salary * 0.10
            - e.base_salary * 0.02
            - 30 * 4325, 2)),                                   -- taxable
        GREATEST(0, ROUND((e.base_salary
            - e.base_salary * 0.10
            - e.base_salary * 0.02
            - 30 * 4325) * 0.10, 2)),                           -- IPN
        ROUND(e.base_salary * 0.10
            + e.base_salary * 0.02
            + GREATEST(0, (e.base_salary
              - e.base_salary * 0.10
              - e.base_salary * 0.02
              - 30 * 4325) * 0.10), 2),                         -- total deductions
        ROUND(e.base_salary
            - e.base_salary * 0.10
            - e.base_salary * 0.02
            - GREATEST(0, (e.base_salary
              - e.base_salary * 0.10
              - e.base_salary * 0.02
              - 30 * 4325) * 0.10), 2),                         -- net
        ROUND((e.base_salary - e.base_salary * 0.10) * 0.05, 2),-- SO
        ROUND(e.base_salary * 0.06, 2),                         -- SN
        ROUND(e.base_salary * 0.035, 2),                        -- ОПВР
        4325,                                                   -- МРП used
        e.is_resident,
        FALSE,
        'APPROVED',
        'seed'
    FROM hrms_employee.employees e
    LEFT JOIN hrms_employee.departments d ON d.id = e.department_id
    LEFT JOIN hrms_employee.positions   p ON p.id = e.position_id
    CROSS JOIN (
        SELECT COUNT(*)::INT AS working_days
        FROM generate_series(last_start, last_end, INTERVAL '1 day') ds
        WHERE EXTRACT(DOW FROM ds) NOT IN (0, 6)
    ) wd
    WHERE e.created_by = 'seed'
      AND e.is_deleted = FALSE
    ON CONFLICT DO NOTHING;

    -- ── Current month: empty DRAFT period (waiting for HR to run generate)
    INSERT INTO payroll_periods (year, month, start_date, end_date, working_days,
        status, created_by)
    VALUES (
        EXTRACT(YEAR  FROM CURRENT_DATE)::INT,
        EXTRACT(MONTH FROM CURRENT_DATE)::INT,
        DATE_TRUNC('month', CURRENT_DATE)::DATE,
        (DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month - 1 day')::DATE,
        (SELECT COUNT(*)::INT FROM generate_series(
            DATE_TRUNC('month', CURRENT_DATE),
            DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month - 1 day',
            INTERVAL '1 day') ds
         WHERE EXTRACT(DOW FROM ds) NOT IN (0, 6)),
        'DRAFT',
        'seed'
    )
    ON CONFLICT DO NOTHING
    RETURNING id INTO cur_period_id;

    -- ── A handful of bonuses + one fine for variety in adjustments view ──
    IF cur_period_id IS NOT NULL THEN
        INSERT INTO payroll_additions (employee_id, period_id, type, category,
            description, amount, is_taxable, created_by) VALUES
            ('33333333-3333-3333-3333-000000000007', cur_period_id, 'BONUS', 'BONUS_PERFORMANCE',
                'Премия за Q1 KPI', 200000, TRUE, 'seed'),
            ('33333333-3333-3333-3333-000000000010', cur_period_id, 'BONUS', 'OVERTIME',
                'Сверхурочные 16ч', 80000, TRUE, 'seed'),
            ('33333333-3333-3333-3333-000000000015', cur_period_id, 'BONUS', 'MEAL_ALLOWANCE',
                'Питание (май)', 35000, FALSE, 'seed'),
            ('33333333-3333-3333-3333-000000000023', cur_period_id, 'DEDUCTION', 'FINE',
                'Опоздания > 3 раз в месяц', 15000, FALSE, 'seed'),
            ('33333333-3333-3333-3333-000000000031', cur_period_id, 'BONUS', 'BONUS_HOLIDAY',
                'Подарок ко Дню Победы', 50000, FALSE, 'seed');
    END IF;

    RAISE NOTICE 'Demo payroll seed: 1 APPROVED period (% / %) with 50 payslips + 1 DRAFT period',
        last_year, last_month;
END $$;