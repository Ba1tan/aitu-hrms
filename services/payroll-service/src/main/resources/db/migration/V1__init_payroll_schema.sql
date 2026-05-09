-- V1 — hrms_payroll schema: payroll_periods, payslips, payroll_additions, salary_advances.
-- Schema `hrms_payroll` is created upfront in scripts/init-db.sql.

-- ─────────────────────────────────────────────────────────────────────────────
-- Payroll periods
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE payroll_periods (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year            INTEGER NOT NULL,
    month           INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    start_date      DATE    NOT NULL,
    end_date        DATE    NOT NULL,
    working_days    INTEGER NOT NULL CHECK (working_days BETWEEN 1 AND 31),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                      CHECK (status IN ('DRAFT','PROCESSING','COMPLETED','APPROVED','PAID','LOCKED')),
    processed_by    UUID,
    processed_at    TIMESTAMP,
    approved_by     UUID,
    approved_at     TIMESTAMP,
    batch_job_id    BIGINT,
    is_deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE UNIQUE INDEX idx_payroll_periods_year_month
    ON payroll_periods(year, month) WHERE is_deleted = FALSE;
CREATE INDEX idx_payroll_periods_status
    ON payroll_periods(status) WHERE is_deleted = FALSE;

-- ─────────────────────────────────────────────────────────────────────────────
-- Payslips — stores employee snapshot (iin, name) so payroll-service does not
-- need a live join with employee-service after the period is processed.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE payslips (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id           UUID NOT NULL REFERENCES payroll_periods(id),
    employee_id         UUID NOT NULL,
    employee_iin        VARCHAR(12),
    employee_name       VARCHAR(300),
    employee_number     VARCHAR(30),
    department_name     VARCHAR(200),
    position_title      VARCHAR(200),

    worked_days         INTEGER NOT NULL,
    total_working_days  INTEGER NOT NULL,

    -- Earnings
    gross_salary        NUMERIC(15,2) NOT NULL,
    earned_salary       NUMERIC(15,2) NOT NULL,
    allowances          NUMERIC(15,2) NOT NULL DEFAULT 0,
    other_deductions    NUMERIC(15,2) NOT NULL DEFAULT 0,

    -- Employee deductions
    opv_amount          NUMERIC(15,2) NOT NULL,
    vosms_amount        NUMERIC(15,2) NOT NULL DEFAULT 0,
    oopv_amount         NUMERIC(15,2) NOT NULL DEFAULT 0,
    taxable_income      NUMERIC(15,2) NOT NULL,
    ipn_amount          NUMERIC(15,2) NOT NULL,
    total_deductions    NUMERIC(15,2) NOT NULL,
    net_salary          NUMERIC(15,2) NOT NULL,

    -- Employer obligations (informational)
    so_amount           NUMERIC(15,2) NOT NULL,
    sn_amount           NUMERIC(15,2) NOT NULL,
    opvr_amount         NUMERIC(15,2) NOT NULL DEFAULT 0,

    -- Calculation metadata
    mrp_used            INTEGER NOT NULL,
    is_resident         BOOLEAN NOT NULL DEFAULT TRUE,
    has_disability      BOOLEAN NOT NULL DEFAULT FALSE,

    -- Status
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                          CHECK (status IN ('DRAFT','FLAGGED','APPROVED','PAID')),

    -- AI anomaly detection
    anomaly_score       NUMERIC(5,4),
    anomaly_flags       TEXT,
    ai_reviewed         BOOLEAN NOT NULL DEFAULT FALSE,
    ai_reviewed_by      UUID,
    ai_reviewed_at      TIMESTAMP,

    pdf_url             TEXT,

    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);
CREATE UNIQUE INDEX idx_payslips_period_employee_unique
    ON payslips(period_id, employee_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_payslips_period       ON payslips(period_id);
CREATE INDEX idx_payslips_employee     ON payslips(employee_id);
CREATE INDEX idx_payslips_status       ON payslips(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_payslips_flagged      ON payslips(period_id) WHERE status = 'FLAGGED' AND is_deleted = FALSE;

-- ─────────────────────────────────────────────────────────────────────────────
-- Payroll additions — bonuses and ad-hoc deductions per (employee, period)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE payroll_additions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL,
    period_id       UUID NOT NULL REFERENCES payroll_periods(id),
    type            VARCHAR(20) NOT NULL CHECK (type IN ('BONUS','DEDUCTION')),
    category        VARCHAR(50) NOT NULL CHECK (category IN (
                        'MEAL_ALLOWANCE','TRANSPORT','OVERTIME','BONUS_PERFORMANCE','BONUS_HOLIDAY',
                        'FINE','ADVANCE_REPAYMENT','TAX_ADJUSTMENT','INSURANCE','OTHER')),
    description     VARCHAR(255),
    amount          NUMERIC(15,2) NOT NULL CHECK (amount >= 0),
    is_taxable      BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE INDEX idx_additions_period_emp ON payroll_additions(period_id, employee_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_additions_employee   ON payroll_additions(employee_id) WHERE is_deleted = FALSE;

-- ─────────────────────────────────────────────────────────────────────────────
-- Salary advances — tracked separately from per-period additions so they can
-- be repaid in installments across multiple periods.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE salary_advances (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL,
    amount          NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    issued_date     DATE NOT NULL,
    repayment_start DATE NOT NULL,
    installments    INTEGER NOT NULL DEFAULT 1 CHECK (installments >= 1),
    remaining       NUMERIC(15,2) NOT NULL CHECK (remaining >= 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE','REPAID','CANCELLED')),
    approved_by     UUID,
    notes           TEXT,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE INDEX idx_advances_employee ON salary_advances(employee_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_advances_status   ON salary_advances(status)      WHERE is_deleted = FALSE;