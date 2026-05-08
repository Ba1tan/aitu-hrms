-- V1 — hrms_leave schema: leave_types, leave_requests, leave_balances, balance_adjustments.
-- Schema `hrms_leave` is created upfront in scripts/init-db.sql.

CREATE TABLE leave_types (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(100) NOT NULL,
    code                 VARCHAR(40)  NOT NULL,
    days_allowed         INT          NOT NULL,
    is_paid              BOOLEAN      NOT NULL DEFAULT TRUE,
    requires_approval    BOOLEAN      NOT NULL DEFAULT TRUE,
    carryover_allowed    BOOLEAN      NOT NULL DEFAULT FALSE,
    carryover_max_days   INT          NOT NULL DEFAULT 0,
    description          TEXT,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP,
    created_by           VARCHAR(255),
    updated_by           VARCHAR(255)
);
CREATE UNIQUE INDEX idx_leave_types_code_unique
    ON leave_types(code) WHERE is_deleted = FALSE;

-- One pending/approved request per (employee, date range). Status flow:
--   PENDING -> APPROVED | REJECTED | CANCELLED
-- days_requested is supplied by the service (calendar-day inclusive count).
CREATE TABLE leave_requests (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id       UUID         NOT NULL,
    leave_type_id     UUID         NOT NULL REFERENCES leave_types(id),
    start_date        DATE         NOT NULL,
    end_date          DATE         NOT NULL,
    days_requested    INT          NOT NULL CHECK (days_requested > 0),
    reason            TEXT,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN (
                          'PENDING','APPROVED','REJECTED','CANCELLED')),
    reviewed_by       UUID,
    reviewed_at       TIMESTAMP,
    review_comment    TEXT,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255),
    CONSTRAINT chk_leave_requests_dates CHECK (end_date >= start_date)
);
CREATE INDEX idx_leave_requests_employee   ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status     ON leave_requests(status);
CREATE INDEX idx_leave_requests_dates      ON leave_requests(start_date, end_date);

-- Balances per (employee, leave_type, year). remaining_days is GENERATED — never
-- write to it directly; mutate the inputs instead. Adjustments append to
-- balance_adjustments, then update adjusted_days.
CREATE TABLE leave_balances (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id       UUID         NOT NULL,
    leave_type_id     UUID         NOT NULL REFERENCES leave_types(id),
    year              INT          NOT NULL,
    entitled_days     INT          NOT NULL DEFAULT 0,
    carried_over      INT          NOT NULL DEFAULT 0,
    used_days         INT          NOT NULL DEFAULT 0,
    adjusted_days     INT          NOT NULL DEFAULT 0,
    remaining_days    INT          GENERATED ALWAYS AS
                          (entitled_days + carried_over + adjusted_days - used_days) STORED,
    is_deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255)
);
CREATE UNIQUE INDEX idx_leave_balances_unique
    ON leave_balances(employee_id, leave_type_id, year) WHERE is_deleted = FALSE;
CREATE INDEX idx_leave_balances_employee ON leave_balances(employee_id);
CREATE INDEX idx_leave_balances_year     ON leave_balances(year);

-- Audit trail for manual balance adjustments.
CREATE TABLE balance_adjustments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    balance_id      UUID         NOT NULL REFERENCES leave_balances(id) ON DELETE CASCADE,
    days            INT          NOT NULL,
    reason          TEXT         NOT NULL,
    adjusted_by     UUID,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE INDEX idx_balance_adjustments_balance ON balance_adjustments(balance_id);

-- Seed default KZ leave types. Annual is the only carryover-eligible type by default.
INSERT INTO leave_types (name, code, days_allowed, is_paid, requires_approval, carryover_allowed, carryover_max_days, description) VALUES
    ('Annual Leave',    'ANNUAL',    24,  TRUE,  TRUE,  TRUE,  12, 'Paid annual vacation'),
    ('Sick Leave',      'SICK',      30,  TRUE,  TRUE,  FALSE,  0, 'Paid medical leave'),
    ('Maternity Leave', 'MATERNITY', 126, TRUE,  TRUE,  FALSE,  0, 'Paid maternity leave (KZ statutory)'),
    ('Unpaid Leave',    'UNPAID',    14,  FALSE, TRUE,  FALSE,  0, 'Unpaid personal leave'),
    ('Study Leave',     'STUDY',     10,  TRUE,  TRUE,  FALSE,  0, 'Education / certification leave');