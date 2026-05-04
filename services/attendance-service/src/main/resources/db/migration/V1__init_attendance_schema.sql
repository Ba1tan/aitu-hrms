-- V1 — hrms_attendance schema: attendance_records, work_schedules, holidays.
-- Schema `hrms_attendance` is created upfront in scripts/init-db.sql.
--
-- Note: biometric enrollment is owned by employee-service (see hrms_employee.biometric_data).
-- This service only consults ai-ml-service for face verification at check-in time.

-- Configurable shifts (one row marked is_default = TRUE for the standard 09:00-18:00 day).
CREATE TABLE work_schedules (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(100)  NOT NULL,
    work_start_time       TIME          NOT NULL,
    work_end_time         TIME          NOT NULL,
    late_threshold_min    INT           NOT NULL DEFAULT 15,
    half_day_threshold_min INT          NOT NULL DEFAULT 240,
    department_id         UUID,
    is_default            BOOLEAN       NOT NULL DEFAULT FALSE,
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);
-- At most one default (and per-department) schedule alive at a time.
CREATE UNIQUE INDEX idx_work_schedules_default
    ON work_schedules(is_default) WHERE is_default = TRUE AND is_deleted = FALSE;
CREATE UNIQUE INDEX idx_work_schedules_department
    ON work_schedules(department_id) WHERE department_id IS NOT NULL AND is_deleted = FALSE;

-- KZ public holidays. is_annual = TRUE means recurring (e.g. Jan 1) — date stored
-- with year for the seeded year and re-used for date-only matching.
CREATE TABLE holidays (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200)  NOT NULL,
    holiday_date DATE         NOT NULL,
    is_annual   BOOLEAN       NOT NULL DEFAULT TRUE,
    description TEXT,
    is_deleted  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);
CREATE UNIQUE INDEX idx_holidays_date_unique
    ON holidays(holiday_date) WHERE is_deleted = FALSE;

-- One record per (employee, work_date). Status & method are checked enums.
CREATE TABLE attendance_records (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id       UUID          NOT NULL,
    work_date         DATE          NOT NULL,
    check_in          TIMESTAMP,
    check_out         TIMESTAMP,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PRESENT' CHECK (status IN (
                          'PRESENT','LATE','ABSENT','HALF_DAY','ON_LEAVE','HOLIDAY','BLOCKED')),
    check_in_method   VARCHAR(20)   CHECK (check_in_method IN (
                          'FACE','MANUAL','WEB','MOBILE','BIOMETRIC')),
    check_out_method  VARCHAR(20)   CHECK (check_out_method IN (
                          'FACE','MANUAL','WEB','MOBILE','BIOMETRIC')),
    location_lat      NUMERIC(9,6),
    location_lng      NUMERIC(9,6),
    worked_minutes    INT,
    overtime_minutes  INT           NOT NULL DEFAULT 0,
    fraud_score       NUMERIC(4,3),
    fraud_flags       VARCHAR(500),
    notes             TEXT,
    is_deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255)
);
CREATE UNIQUE INDEX idx_attendance_employee_date_unique
    ON attendance_records(employee_id, work_date) WHERE is_deleted = FALSE;
CREATE INDEX idx_attendance_work_date    ON attendance_records(work_date);
CREATE INDEX idx_attendance_status       ON attendance_records(status);
CREATE INDEX idx_attendance_employee_recent
    ON attendance_records(employee_id, created_at DESC);

-- Default schedule (09:00-18:00, 15-min late grace).
INSERT INTO work_schedules (name, work_start_time, work_end_time, late_threshold_min, is_default)
VALUES ('Standard 9-18', '09:00:00', '18:00:00', 15, TRUE);