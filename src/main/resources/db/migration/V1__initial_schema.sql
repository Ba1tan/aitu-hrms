-- V1__initial_schema.sql
-- HRMS Initial Database Schema
-- Created for Kazakhstan SMB HR Management System

-- ===== USERS =====
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password        VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL CHECK (role IN ('SUPER_ADMIN','HR_MANAGER','ACCOUNTANT','EMPLOYEE')),
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN      NOT NULL DEFAULT TRUE,
    employee_id     UUID,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

-- ===== DEPARTMENTS =====
CREATE TABLE departments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150)    NOT NULL UNIQUE,
    description TEXT,
    cost_center VARCHAR(50),
    manager_id  UUID,
    parent_id   UUID            REFERENCES departments(id),
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

-- ===== POSITIONS =====
CREATE TABLE positions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(150)    NOT NULL,
    description   TEXT,
    min_salary    NUMERIC(15,2),
    max_salary    NUMERIC(15,2),
    department_id UUID            REFERENCES departments(id),
    is_deleted    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

-- ===== EMPLOYEES =====
CREATE TABLE employees (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_number   VARCHAR(20)     NOT NULL UNIQUE,
    first_name        VARCHAR(100)    NOT NULL,
    last_name         VARCHAR(100)    NOT NULL,
    middle_name       VARCHAR(100),
    date_of_birth     DATE,
    iin               VARCHAR(12)     UNIQUE,
    email             VARCHAR(255)    NOT NULL UNIQUE,
    phone             VARCHAR(20),
    hire_date         DATE            NOT NULL,
    termination_date  DATE,
    status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                          CHECK (status IN ('ACTIVE','ON_LEAVE','TERMINATED','PROBATION')),
    employment_type   VARCHAR(20)     NOT NULL DEFAULT 'FULL_TIME'
                          CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERN')),
    base_salary       NUMERIC(15,2)   NOT NULL,
    department_id     UUID            REFERENCES departments(id),
    position_id       UUID            REFERENCES positions(id),
    manager_id        UUID            REFERENCES employees(id),
    bank_account      VARCHAR(34),
    bank_name         VARCHAR(150),
    profile_photo_url TEXT,
    is_resident       BOOLEAN         NOT NULL DEFAULT TRUE,
    has_disability    BOOLEAN         NOT NULL DEFAULT FALSE,
    is_pensioner      BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255)
);

-- Add FK from departments to employee manager
ALTER TABLE departments ADD CONSTRAINT fk_dept_manager
    FOREIGN KEY (manager_id) REFERENCES employees(id);

-- Add FK from users to employees
ALTER TABLE users ADD CONSTRAINT fk_user_employee
    FOREIGN KEY (employee_id) REFERENCES employees(id);

-- ===== ATTENDANCE =====
CREATE TABLE attendance_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID            NOT NULL REFERENCES employees(id),
    work_date       DATE            NOT NULL,
    check_in        TIMESTAMP,
    check_out       TIMESTAMP,
    worked_hours    NUMERIC(5,2),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PRESENT'
                        CHECK (status IN ('PRESENT','ABSENT','LATE','HALF_DAY','HOLIDAY','WEEKEND')),
    note            TEXT,
    approved_by     UUID            REFERENCES users(id),
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    UNIQUE (employee_id, work_date)
);

-- ===== LEAVE TYPES =====
CREATE TABLE leave_types (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100)    NOT NULL UNIQUE,
    days_allowed    INTEGER         NOT NULL,
    is_paid         BOOLEAN         NOT NULL DEFAULT TRUE,
    description     TEXT,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

-- ===== LEAVE REQUESTS =====
CREATE TABLE leave_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID            NOT NULL REFERENCES employees(id),
    leave_type_id   UUID            NOT NULL REFERENCES leave_types(id),
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    days_requested  INTEGER         NOT NULL,
    reason          TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    reviewed_by     UUID            REFERENCES users(id),
    reviewed_at     TIMESTAMP,
    review_comment  TEXT,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

-- ===== LEAVE BALANCES =====
CREATE TABLE leave_balances (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID            NOT NULL REFERENCES employees(id),
    leave_type_id   UUID            NOT NULL REFERENCES leave_types(id),
    year            INTEGER         NOT NULL,
    entitled_days   INTEGER         NOT NULL,
    used_days       INTEGER         NOT NULL DEFAULT 0,
    remaining_days  INTEGER         GENERATED ALWAYS AS (entitled_days - used_days) STORED,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    UNIQUE (employee_id, leave_type_id, year)
);

-- ===== PAYROLL PERIODS =====
CREATE TABLE payroll_periods (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year            INTEGER         NOT NULL,
    month           INTEGER         NOT NULL CHECK (month BETWEEN 1 AND 12),
    start_date      DATE            NOT NULL,
    end_date        DATE            NOT NULL,
    working_days    INTEGER         NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','PROCESSING','APPROVED','PAID','LOCKED')),
    processed_by    UUID            REFERENCES users(id),
    processed_at    TIMESTAMP,
    approved_by     UUID            REFERENCES users(id),
    approved_at     TIMESTAMP,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    UNIQUE (year, month)
);

-- ===== PAYSLIPS =====
CREATE TABLE payslips (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id           UUID            NOT NULL REFERENCES payroll_periods(id),
    employee_id         UUID            NOT NULL REFERENCES employees(id),
    worked_days         INTEGER         NOT NULL,
    total_working_days  INTEGER         NOT NULL,
    gross_salary        NUMERIC(15,2)   NOT NULL,
    earned_salary       NUMERIC(15,2)   NOT NULL,
    allowances          NUMERIC(15,2)   NOT NULL DEFAULT 0,
    other_deductions    NUMERIC(15,2)   NOT NULL DEFAULT 0,
    opv_amount          NUMERIC(15,2)   NOT NULL,
    oopv_amount         NUMERIC(15,2)   NOT NULL DEFAULT 0,
    taxable_income      NUMERIC(15,2)   NOT NULL,
    ipn_amount          NUMERIC(15,2)   NOT NULL,
    total_deductions    NUMERIC(15,2)   NOT NULL,
    net_salary          NUMERIC(15,2)   NOT NULL,
    so_amount           NUMERIC(15,2)   NOT NULL,
    sn_amount           NUMERIC(15,2)   NOT NULL,
    mrp_used            INTEGER         NOT NULL,
    is_resident         BOOLEAN         NOT NULL DEFAULT TRUE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT','APPROVED','PAID')),
    pdf_url             TEXT,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    UNIQUE (period_id, employee_id)
);

-- ===== INDEXES =====
CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employees_iin ON employees(iin);
CREATE INDEX idx_attendance_employee_date ON attendance_records(employee_id, work_date);
CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_payslips_period ON payslips(period_id);
CREATE INDEX idx_payslips_employee ON payslips(employee_id);

-- ===== SEED DATA =====
-- Default leave types per Kazakhstan Labour Code
INSERT INTO leave_types (id, name, days_allowed, is_paid, description)
VALUES
    (gen_random_uuid(), 'Annual Leave',      24, TRUE,  'Ежегодный оплачиваемый отпуск (Art. 88 Labour Code)'),
    (gen_random_uuid(), 'Sick Leave',        30, TRUE,  'Больничный лист'),
    (gen_random_uuid(), 'Maternity Leave',  126, TRUE,  'Декретный отпуск (70 days pre + 56 days post)'),
    (gen_random_uuid(), 'Unpaid Leave',      14, FALSE, 'Отпуск без сохранения заработной платы'),
    (gen_random_uuid(), 'Study Leave',       10, TRUE,  'Учебный отпуск');
