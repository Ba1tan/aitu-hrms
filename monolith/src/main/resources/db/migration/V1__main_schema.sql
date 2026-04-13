

-- 1. USERS & AUTH


CREATE TABLE users (
   id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   first_name          VARCHAR(100)    NOT NULL,
   last_name           VARCHAR(100)    NOT NULL,
   email               VARCHAR(255)    NOT NULL UNIQUE,
   password            VARCHAR(255)    NOT NULL,
   role                VARCHAR(20)     NOT NULL CHECK (role IN (
                                                                'SUPER_ADMIN','DIRECTOR','HR_MANAGER','HR_SPECIALIST',
                                                                'ACCOUNTANT','MANAGER','TEAM_LEAD','EMPLOYEE')),
   enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
   account_non_locked  BOOLEAN         NOT NULL DEFAULT TRUE,
   employee_id         UUID,
   require_password_change BOOLEAN     NOT NULL DEFAULT FALSE,
   last_login_at       TIMESTAMP,
   last_login_ip       VARCHAR(45),
   failed_login_count  INTEGER         NOT NULL DEFAULT 0,
   locked_until        TIMESTAMP,
   is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
   created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
   updated_at          TIMESTAMP,
   created_by          VARCHAR(255),
   updated_by          VARCHAR(255)
);

-- Permissions (RBAC)
CREATE TABLE permissions (
         id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
         code        VARCHAR(100) NOT NULL UNIQUE,
         description TEXT,
         module      VARCHAR(50) NOT NULL
);

CREATE TABLE role_permissions (
              role          VARCHAR(20) NOT NULL,
              permission_id UUID NOT NULL REFERENCES permissions(id),
              PRIMARY KEY (role, permission_id)
);

-- Audit log
CREATE TABLE audit_logs (
        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id     UUID,
        user_email  VARCHAR(255),
        action      VARCHAR(50) NOT NULL,
        entity_type VARCHAR(50) NOT NULL,
        entity_id   UUID,
        old_value   JSONB,
        new_value   JSONB,
        ip_address  VARCHAR(45),
        user_agent  TEXT,
        created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_date ON audit_logs(created_at);


-- 2. EMPLOYEES. ORGANIZATION


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
       termination_reason VARCHAR(255),
       status            VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
           CHECK (status IN ('ACTIVE','ON_LEAVE','TERMINATED','PROBATION','SUSPENDED')),
       employment_type   VARCHAR(20)     NOT NULL DEFAULT 'FULL_TIME'
           CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERN')),
       base_salary       NUMERIC(15,2)   NOT NULL,
       department_id     UUID            REFERENCES departments(id),
       position_id       UUID            REFERENCES positions(id),
       manager_id        UUID            REFERENCES employees(id),
       bank_account      VARCHAR(34),
       bank_name         VARCHAR(150),
       profile_photo_url TEXT,
       address           TEXT,
       is_resident       BOOLEAN         NOT NULL DEFAULT TRUE,
       has_disability    BOOLEAN         NOT NULL DEFAULT FALSE,
       disability_group  INTEGER         CHECK (disability_group IN (1, 2, 3)),
       is_pensioner      BOOLEAN         NOT NULL DEFAULT FALSE,
       is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
       created_at        TIMESTAMP       NOT NULL DEFAULT NOW(),
       updated_at        TIMESTAMP,
       created_by        VARCHAR(255),
       updated_by        VARCHAR(255)
);

ALTER TABLE departments ADD CONSTRAINT fk_dept_manager
FOREIGN KEY (manager_id) REFERENCES employees(id);
ALTER TABLE users ADD CONSTRAINT fk_user_employee
FOREIGN KEY (employee_id) REFERENCES employees(id);

-- Salary history
CREATE TABLE salary_history (
            id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            employee_id     UUID NOT NULL REFERENCES employees(id),
            previous_salary NUMERIC(15,2) NOT NULL,
            new_salary      NUMERIC(15,2) NOT NULL,
            effective_date  DATE NOT NULL,
            reason          VARCHAR(50) NOT NULL CHECK (reason IN (
                                                                   'HIRING','PROMOTION','ANNUAL_REVIEW','CONTRACT_RENEWAL','CORRECTION','DEMOTION','OTHER')),
            notes           TEXT,
            approved_by     UUID,
            is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
            created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMP,
            created_by      VARCHAR(255),
            updated_by      VARCHAR(255)
);

-- Employee documents
CREATE TABLE employee_documents (
                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                employee_id     UUID NOT NULL REFERENCES employees(id),
                document_type   VARCHAR(50) NOT NULL CHECK (document_type IN (
                                                                              'EMPLOYMENT_CONTRACT','ID_CARD','PASSPORT','DIPLOMA',
                                                                              'MEDICAL_CERTIFICATE','TAX_REGISTRATION','BANK_DETAILS','OTHER')),
                file_name       VARCHAR(255) NOT NULL,
                file_url        TEXT NOT NULL,
                file_size       BIGINT,
                mime_type       VARCHAR(100),
                uploaded_by     UUID,
                expiry_date     DATE,
                is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
                created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at      TIMESTAMP,
                created_by      VARCHAR(255),
                updated_by      VARCHAR(255)
);

-- Emergency contacts
CREATE TABLE emergency_contacts (
                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                employee_id     UUID NOT NULL REFERENCES employees(id),
                name            VARCHAR(200) NOT NULL,
                relationship    VARCHAR(50) NOT NULL,
                phone           VARCHAR(20) NOT NULL,
                email           VARCHAR(255),
                is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
                is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
                created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
                updated_at      TIMESTAMP,
                created_by      VARCHAR(255),
                updated_by      VARCHAR(255)
);

-- 3. ATTENDANCE


CREATE TABLE work_schedules (
            id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            name                VARCHAR(100) NOT NULL,
            work_start_time     TIME NOT NULL DEFAULT '09:00',
            work_end_time       TIME NOT NULL DEFAULT '18:00',
            late_threshold_min  INTEGER NOT NULL DEFAULT 10,
            break_duration_min  INTEGER NOT NULL DEFAULT 60,
            working_hours       NUMERIC(4,2) NOT NULL DEFAULT 8.00,
            is_default          BOOLEAN NOT NULL DEFAULT FALSE,
            is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
            created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at          TIMESTAMP,
            created_by          VARCHAR(255),
            updated_by          VARCHAR(255)
);

CREATE TABLE holidays (
      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      name        VARCHAR(200) NOT NULL,
      date        DATE NOT NULL,
      is_annual   BOOLEAN NOT NULL DEFAULT FALSE,
      description TEXT,
      is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
      created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
      updated_at  TIMESTAMP,
      created_by  VARCHAR(255),
      updated_by  VARCHAR(255)
);

CREATE TABLE attendance_records (
                id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                employee_id     UUID            NOT NULL REFERENCES employees(id),
                work_date       DATE            NOT NULL,
                check_in        TIMESTAMP,
                check_out       TIMESTAMP,
                worked_hours    NUMERIC(5,2),
                overtime_hours  NUMERIC(5,2)    DEFAULT 0,
                status          VARCHAR(20)     NOT NULL DEFAULT 'PRESENT'
                    CHECK (status IN ('PRESENT','ABSENT','LATE','HALF_DAY','HOLIDAY','WEEKEND','ON_LEAVE')),
                check_in_method VARCHAR(20)     DEFAULT 'MANUAL'
                    CHECK (check_in_method IN ('MANUAL','WEB','MOBILE','BIOMETRIC')),
                device_id       VARCHAR(50),
                location_lat    NUMERIC(10,7),
                location_lng    NUMERIC(10,7),
                fraud_score     NUMERIC(5,4),
                fraud_flags     TEXT,
                note            TEXT,
                approved_by     UUID            REFERENCES users(id),
                is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
                created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
                updated_at      TIMESTAMP,
                created_by      VARCHAR(255),
                updated_by      VARCHAR(255),
                UNIQUE (employee_id, work_date)
);

CREATE TABLE biometric_data (
            id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            employee_id      UUID NOT NULL UNIQUE REFERENCES employees(id),
            fingerprint_hash VARCHAR(128) NOT NULL,
            enrolled_at      TIMESTAMP NOT NULL DEFAULT NOW(),
            enrolled_by      UUID,
            is_active        BOOLEAN NOT NULL DEFAULT TRUE
);

-- 4. LEAVE


CREATE TABLE leave_types (
         id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
         name              VARCHAR(100) NOT NULL UNIQUE,
         days_allowed      INTEGER NOT NULL,
         is_paid           BOOLEAN NOT NULL DEFAULT TRUE,
         requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
         max_consecutive   INTEGER,
         carryover_allowed BOOLEAN NOT NULL DEFAULT FALSE,
         carryover_max_days INTEGER DEFAULT 0,
         description       TEXT,
         is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
         created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
         updated_at        TIMESTAMP,
         created_by        VARCHAR(255),
         updated_by        VARCHAR(255)
);

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
            cancelled_at    TIMESTAMP,
            cancel_reason   TEXT,
            is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMP,
            created_by      VARCHAR(255),
            updated_by      VARCHAR(255)
);

CREATE TABLE leave_balances (
            id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            employee_id     UUID            NOT NULL REFERENCES employees(id),
            leave_type_id   UUID            NOT NULL REFERENCES leave_types(id),
            year            INTEGER         NOT NULL,
            entitled_days   INTEGER         NOT NULL,
            used_days       INTEGER         NOT NULL DEFAULT 0,
            carried_over    INTEGER         NOT NULL DEFAULT 0,
            adjusted_days   INTEGER         NOT NULL DEFAULT 0,
            remaining_days  INTEGER         GENERATED ALWAYS AS (entitled_days + carried_over + adjusted_days - used_days) STORED,
            is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
            created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
            updated_at      TIMESTAMP,
            created_by      VARCHAR(255),
            updated_by      VARCHAR(255),
            UNIQUE (employee_id, leave_type_id, year)
);

CREATE TABLE balance_adjustments (
                 id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                 balance_id  UUID NOT NULL REFERENCES leave_balances(id),
                 adjustment  INTEGER NOT NULL,
                 reason      TEXT NOT NULL,
                 adjusted_by UUID NOT NULL,
                 created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);


-- 5. PAYROLL


CREATE TABLE payroll_periods (
             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
             year            INTEGER         NOT NULL,
             month           INTEGER         NOT NULL CHECK (month BETWEEN 1 AND 12),
start_date      DATE            NOT NULL,
end_date        DATE            NOT NULL,
working_days    INTEGER         NOT NULL,
status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
  CHECK (status IN ('DRAFT','PROCESSING','COMPLETED','APPROVED','PAID','LOCKED')),
processed_by    UUID            REFERENCES users(id),
processed_at    TIMESTAMP,
approved_by     UUID            REFERENCES users(id),
approved_at     TIMESTAMP,
batch_job_id    BIGINT,
is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
updated_at      TIMESTAMP,
created_by      VARCHAR(255),
updated_by      VARCHAR(255),
UNIQUE (year, month)
);

CREATE TABLE payslips (
      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      period_id           UUID            NOT NULL REFERENCES payroll_periods(id),
      employee_id         UUID            NOT NULL REFERENCES employees(id),
      employee_iin        VARCHAR(12),
      employee_name       VARCHAR(300),
      worked_days         INTEGER         NOT NULL,
      total_working_days  INTEGER         NOT NULL,
      gross_salary        NUMERIC(15,2)   NOT NULL,
      earned_salary       NUMERIC(15,2)   NOT NULL,
      allowances          NUMERIC(15,2)   NOT NULL DEFAULT 0,
      other_deductions    NUMERIC(15,2)   NOT NULL DEFAULT 0,
      opv_amount          NUMERIC(15,2)   NOT NULL,
      oopv_amount         NUMERIC(15,2)   NOT NULL DEFAULT 0,
      vosms_amount        NUMERIC(15,2)   NOT NULL DEFAULT 0,
      opvr_amount         NUMERIC(15,2)   NOT NULL DEFAULT 0,
      taxable_income      NUMERIC(15,2)   NOT NULL,
      ipn_amount          NUMERIC(15,2)   NOT NULL,
      total_deductions    NUMERIC(15,2)   NOT NULL,
      net_salary          NUMERIC(15,2)   NOT NULL,
      so_amount           NUMERIC(15,2)   NOT NULL,
      sn_amount           NUMERIC(15,2)   NOT NULL,
      mrp_used            INTEGER         NOT NULL,
      is_resident         BOOLEAN         NOT NULL DEFAULT TRUE,
      has_disability      BOOLEAN         NOT NULL DEFAULT FALSE,
      status              VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
          CHECK (status IN ('DRAFT','FLAGGED','APPROVED','PAID')),
      anomaly_score       NUMERIC(5,4),
      anomaly_flags       TEXT,
      ai_reviewed         BOOLEAN         NOT NULL DEFAULT FALSE,
      ai_reviewed_by      UUID,
      ai_reviewed_at      TIMESTAMP,
      pdf_url             TEXT,
      is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
      created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
      updated_at          TIMESTAMP,
      created_by          VARCHAR(255),
      updated_by          VARCHAR(255),
      UNIQUE (period_id, employee_id)
);

CREATE TABLE payroll_additions (
               id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
               employee_id UUID NOT NULL REFERENCES employees(id),
               period_id   UUID NOT NULL REFERENCES payroll_periods(id),
               type        VARCHAR(20) NOT NULL CHECK (type IN ('BONUS', 'DEDUCTION')),
               category    VARCHAR(50) NOT NULL CHECK (category IN (
                                                                    'MEAL_ALLOWANCE','TRANSPORT','OVERTIME','BONUS_PERFORMANCE','BONUS_HOLIDAY',
                                                                    'FINE','ADVANCE_REPAYMENT','TAX_ADJUSTMENT','INSURANCE','OTHER')),
               description VARCHAR(255),
               amount      NUMERIC(15,2) NOT NULL,
               is_taxable  BOOLEAN NOT NULL DEFAULT TRUE,
               is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
               created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
               updated_at  TIMESTAMP,
               created_by  VARCHAR(255),
               updated_by  VARCHAR(255)
);

CREATE TABLE salary_advances (
             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
             employee_id     UUID NOT NULL REFERENCES employees(id),
             amount          NUMERIC(15,2) NOT NULL,
             issued_date     DATE NOT NULL,
             repayment_start DATE NOT NULL,
             installments    INTEGER NOT NULL DEFAULT 1,
             remaining       NUMERIC(15,2) NOT NULL,
             status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE','REPAID','CANCELLED')),
             approved_by     UUID,
             is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
             created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
             updated_at      TIMESTAMP,
             created_by      VARCHAR(255),
             updated_by      VARCHAR(255)
);

-- 6. NOTIFICATIONS


CREATE TABLE notifications (
           id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
           user_id         UUID NOT NULL REFERENCES users(id),
           title           VARCHAR(255) NOT NULL,
           message         TEXT NOT NULL,
           type            VARCHAR(30) NOT NULL CHECK (type IN (
                                                                'LEAVE_REQUEST','LEAVE_APPROVED','LEAVE_REJECTED',
                                                                'PAYROLL_READY','PAYSLIP_GENERATED','PAYROLL_ANOMALY',
                                                                'ATTENDANCE_ALERT','FRAUD_ALERT',
                                                                'EMPLOYEE_ONBOARDED','EMPLOYEE_TERMINATED',
                                                                'PASSWORD_RESET','SYSTEM','INFO')),
           channel         VARCHAR(20) NOT NULL DEFAULT 'IN_APP'
               CHECK (channel IN ('IN_APP','EMAIL','SMS','PUSH')),
           is_read         BOOLEAN NOT NULL DEFAULT FALSE,
           read_at         TIMESTAMP,
           reference_type  VARCHAR(50),
           reference_id    UUID,
           is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
           created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 7. INTEGRATION


CREATE TABLE sync_jobs (
       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       period_id       UUID NOT NULL REFERENCES payroll_periods(id),
       target          VARCHAR(20) NOT NULL CHECK (target IN ('ONE_C', 'BANK')),
       status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
           CHECK (status IN ('PENDING','IN_PROGRESS','SUCCESS','FAILED','RETRYING')),
       payload         JSONB,
       response        JSONB,
       onec_document_id VARCHAR(100),
       error_message   TEXT,
       retry_count     INTEGER NOT NULL DEFAULT 0,
       max_retries     INTEGER NOT NULL DEFAULT 3,
       next_retry_at   TIMESTAMP,
       completed_at    TIMESTAMP,
       is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
       created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
       updated_at      TIMESTAMP,
       created_by      VARCHAR(255),
       updated_by      VARCHAR(255)
);

CREATE TABLE company_settings (
              id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
              key         VARCHAR(100) NOT NULL UNIQUE,
              value       TEXT NOT NULL,
              description TEXT,
              category    VARCHAR(50) NOT NULL,
              updated_at  TIMESTAMP,
              updated_by  VARCHAR(255)
);

-- 8. INDEXES


CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_status ON employees(status) WHERE is_deleted = false;
CREATE INDEX idx_employees_manager ON employees(manager_id);
CREATE INDEX idx_employees_iin ON employees(iin);
CREATE INDEX idx_employees_name_search ON employees(lower(last_name), lower(first_name)) WHERE is_deleted = false;
CREATE INDEX idx_salary_history_emp ON salary_history(employee_id);
CREATE INDEX idx_documents_emp ON employee_documents(employee_id);
CREATE INDEX idx_attendance_emp_date ON attendance_records(employee_id, work_date);
CREATE INDEX idx_attendance_date ON attendance_records(work_date) WHERE is_deleted = false;
CREATE INDEX idx_holidays_date ON holidays(date);
CREATE INDEX idx_leave_req_emp ON leave_requests(employee_id);
CREATE INDEX idx_leave_req_status ON leave_requests(status);
CREATE INDEX idx_leave_req_pending ON leave_requests(status) WHERE status = 'PENDING' AND is_deleted = false;
CREATE INDEX idx_leave_req_dates ON leave_requests(start_date, end_date) WHERE is_deleted = false;
CREATE INDEX idx_leave_bal_emp ON leave_balances(employee_id, year);
CREATE INDEX idx_payslips_period ON payslips(period_id);
CREATE INDEX idx_payslips_employee ON payslips(employee_id);
CREATE INDEX idx_payslips_period_status ON payslips(period_id, status) WHERE is_deleted = false;
CREATE INDEX idx_payslips_flagged ON payslips(period_id) WHERE status = 'FLAGGED';
CREATE INDEX idx_additions_period ON payroll_additions(period_id, employee_id);
CREATE INDEX idx_notif_user ON notifications(user_id);
CREATE INDEX idx_notif_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_sync_period ON sync_jobs(period_id);
CREATE INDEX idx_sync_status ON sync_jobs(status);

-- 9. SEED: PERMISSIONS (32)


INSERT INTO permissions (id, code, description, module) VALUES
            (gen_random_uuid(), 'EMPLOYEE_VIEW_OWN', 'View own profile', 'EMPLOYEE'),
            (gen_random_uuid(), 'EMPLOYEE_VIEW_TEAM', 'View direct reports', 'EMPLOYEE'),
            (gen_random_uuid(), 'EMPLOYEE_VIEW_ALL', 'View all employees', 'EMPLOYEE'),
            (gen_random_uuid(), 'EMPLOYEE_CREATE', 'Create new employee', 'EMPLOYEE'),
            (gen_random_uuid(), 'EMPLOYEE_UPDATE', 'Edit employee data', 'EMPLOYEE'),
            (gen_random_uuid(), 'EMPLOYEE_DELETE', 'Terminate/delete employee', 'EMPLOYEE'),
            (gen_random_uuid(), 'EMPLOYEE_SALARY_VIEW', 'View salary information', 'EMPLOYEE'),
            (gen_random_uuid(), 'EMPLOYEE_SALARY_CHANGE', 'Modify salary', 'EMPLOYEE'),
            (gen_random_uuid(), 'PAYROLL_VIEW', 'View payroll periods/payslips', 'PAYROLL'),
            (gen_random_uuid(), 'PAYROLL_PROCESS', 'Generate payslips', 'PAYROLL'),
            (gen_random_uuid(), 'PAYROLL_APPROVE', 'Approve payroll period', 'PAYROLL'),
            (gen_random_uuid(), 'PAYROLL_PAY', 'Mark period as paid', 'PAYROLL'),
            (gen_random_uuid(), 'PAYSLIP_VIEW_OWN', 'View own payslips', 'PAYROLL'),
            (gen_random_uuid(), 'PAYSLIP_ADJUST', 'Adjust individual payslip', 'PAYROLL'),
            (gen_random_uuid(), 'LEAVE_REQUEST_OWN', 'Submit own leave request', 'LEAVE'),
            (gen_random_uuid(), 'LEAVE_APPROVE_TEAM', 'Approve team leave requests', 'LEAVE'),
            (gen_random_uuid(), 'LEAVE_APPROVE_ALL', 'Approve any leave request', 'LEAVE'),
            (gen_random_uuid(), 'LEAVE_BALANCE_MANAGE', 'Adjust leave balances', 'LEAVE'),
            (gen_random_uuid(), 'ATTENDANCE_CHECKIN', 'Check in/out', 'ATTENDANCE'),
            (gen_random_uuid(), 'ATTENDANCE_VIEW_TEAM', 'View team attendance', 'ATTENDANCE'),
            (gen_random_uuid(), 'ATTENDANCE_VIEW_ALL', 'View all attendance records', 'ATTENDANCE'),
            (gen_random_uuid(), 'ATTENDANCE_MANAGE', 'Create/edit attendance records', 'ATTENDANCE'),
            (gen_random_uuid(), 'REPORT_PAYROLL', 'Download payroll reports', 'REPORT'),
            (gen_random_uuid(), 'REPORT_ATTENDANCE', 'Download attendance reports', 'REPORT'),
            (gen_random_uuid(), 'REPORT_LEAVE', 'Download leave reports', 'REPORT'),
            (gen_random_uuid(), 'REPORT_FORM200', 'Generate Form 200.00', 'REPORT'),
            (gen_random_uuid(), 'REPORT_EXECUTIVE', 'View executive dashboards', 'REPORT'),
            (gen_random_uuid(), 'SYSTEM_SETTINGS', 'Manage system settings', 'SYSTEM'),
            (gen_random_uuid(), 'SYSTEM_USERS', 'Manage user accounts', 'SYSTEM'),
            (gen_random_uuid(), 'SYSTEM_AUDIT', 'View audit logs', 'SYSTEM'),
            (gen_random_uuid(), 'SYSTEM_ROLES', 'Manage role permissions', 'SYSTEM'),
            (gen_random_uuid(), 'AI_DASHBOARD', 'View AI insights dashboard', 'AI');

-- Role → Permission mappings
INSERT INTO role_permissions (role, permission_id) SELECT 'SUPER_ADMIN', id FROM permissions;

INSERT INTO role_permissions (role, permission_id) SELECT 'DIRECTOR', id FROM permissions WHERE code IN (
         'EMPLOYEE_VIEW_OWN','EMPLOYEE_VIEW_TEAM','EMPLOYEE_VIEW_ALL','EMPLOYEE_SALARY_VIEW',
         'PAYROLL_VIEW','PAYSLIP_VIEW_OWN','LEAVE_REQUEST_OWN',
         'ATTENDANCE_CHECKIN','ATTENDANCE_VIEW_TEAM','ATTENDANCE_VIEW_ALL',
         'REPORT_PAYROLL','REPORT_ATTENDANCE','REPORT_LEAVE','REPORT_EXECUTIVE','AI_DASHBOARD');

INSERT INTO role_permissions (role, permission_id) SELECT 'HR_MANAGER', id FROM permissions WHERE code IN (
           'EMPLOYEE_VIEW_OWN','EMPLOYEE_VIEW_TEAM','EMPLOYEE_VIEW_ALL',
           'EMPLOYEE_CREATE','EMPLOYEE_UPDATE','EMPLOYEE_DELETE','EMPLOYEE_SALARY_VIEW','EMPLOYEE_SALARY_CHANGE',
           'PAYROLL_VIEW','PAYROLL_PROCESS','PAYROLL_APPROVE','PAYSLIP_VIEW_OWN','PAYSLIP_ADJUST',
           'LEAVE_REQUEST_OWN','LEAVE_APPROVE_TEAM','LEAVE_APPROVE_ALL','LEAVE_BALANCE_MANAGE',
           'ATTENDANCE_CHECKIN','ATTENDANCE_VIEW_TEAM','ATTENDANCE_VIEW_ALL','ATTENDANCE_MANAGE',
           'REPORT_PAYROLL','REPORT_ATTENDANCE','REPORT_LEAVE','REPORT_FORM200','SYSTEM_AUDIT','AI_DASHBOARD');

INSERT INTO role_permissions (role, permission_id) SELECT 'HR_SPECIALIST', id FROM permissions WHERE code IN (
              'EMPLOYEE_VIEW_OWN','EMPLOYEE_VIEW_TEAM','EMPLOYEE_VIEW_ALL','EMPLOYEE_CREATE','EMPLOYEE_UPDATE',
              'PAYSLIP_VIEW_OWN','LEAVE_REQUEST_OWN','LEAVE_APPROVE_TEAM','LEAVE_APPROVE_ALL',
              'ATTENDANCE_CHECKIN','ATTENDANCE_VIEW_TEAM','ATTENDANCE_VIEW_ALL','ATTENDANCE_MANAGE',
              'REPORT_ATTENDANCE','REPORT_LEAVE');

INSERT INTO role_permissions (role, permission_id) SELECT 'ACCOUNTANT', id FROM permissions WHERE code IN (
           'EMPLOYEE_VIEW_OWN','EMPLOYEE_SALARY_VIEW',
           'PAYROLL_VIEW','PAYROLL_PAY','PAYSLIP_VIEW_OWN','PAYSLIP_ADJUST',
           'LEAVE_REQUEST_OWN','ATTENDANCE_CHECKIN',
           'REPORT_PAYROLL','REPORT_FORM200','SYSTEM_AUDIT');

INSERT INTO role_permissions (role, permission_id) SELECT 'MANAGER', id FROM permissions WHERE code IN (
        'EMPLOYEE_VIEW_OWN','EMPLOYEE_VIEW_TEAM','PAYSLIP_VIEW_OWN',
        'LEAVE_REQUEST_OWN','LEAVE_APPROVE_TEAM',
        'ATTENDANCE_CHECKIN','ATTENDANCE_VIEW_TEAM',
        'REPORT_ATTENDANCE','REPORT_LEAVE');

INSERT INTO role_permissions (role, permission_id) SELECT 'TEAM_LEAD', id FROM permissions WHERE code IN (
          'EMPLOYEE_VIEW_OWN','EMPLOYEE_VIEW_TEAM','PAYSLIP_VIEW_OWN',
          'LEAVE_REQUEST_OWN','LEAVE_APPROVE_TEAM',
          'ATTENDANCE_CHECKIN','ATTENDANCE_VIEW_TEAM');

INSERT INTO role_permissions (role, permission_id) SELECT 'EMPLOYEE', id FROM permissions WHERE code IN (
         'EMPLOYEE_VIEW_OWN','PAYSLIP_VIEW_OWN','LEAVE_REQUEST_OWN','ATTENDANCE_CHECKIN');

-- 10. SEED: LEAVE TYPES


INSERT INTO leave_types (id, name, days_allowed, is_paid, requires_approval, carryover_allowed, carryover_max_days, description) VALUES
         (gen_random_uuid(), 'Annual Leave',      24, TRUE,  TRUE,  TRUE,  12, 'Ежегодный оплачиваемый отпуск (Art. 88 Labour Code)'),
         (gen_random_uuid(), 'Sick Leave',        30, TRUE,  TRUE,  FALSE, 0,  'Больничный лист'),
         (gen_random_uuid(), 'Maternity Leave',  126, TRUE,  TRUE,  FALSE, 0,  'Декретный отпуск (70 days pre + 56 days post)'),
         (gen_random_uuid(), 'Unpaid Leave',      14, FALSE, TRUE,  FALSE, 0,  'Отпуск без сохранения заработной платы'),
         (gen_random_uuid(), 'Study Leave',       10, TRUE,  TRUE,  FALSE, 0,  'Учебный отпуск');


-- 11. SEED: HOLIDAYS


INSERT INTO holidays (id, name, date, is_annual) VALUES
         (gen_random_uuid(), 'Новый год',                            '2026-01-01', true),
         (gen_random_uuid(), 'Новый год',                            '2026-01-02', true),
         (gen_random_uuid(), 'Международный женский день',            '2026-03-08', true),
         (gen_random_uuid(), 'Наурыз мейрамы',                       '2026-03-21', true),
         (gen_random_uuid(), 'Наурыз мейрамы',                       '2026-03-22', true),
         (gen_random_uuid(), 'Наурыз мейрамы',                       '2026-03-23', true),
         (gen_random_uuid(), 'Праздник единства народа Казахстана',   '2026-05-01', true),
         (gen_random_uuid(), 'День Защитника Отечества',              '2026-05-07', true),
         (gen_random_uuid(), 'День Победы',                           '2026-05-09', true),
         (gen_random_uuid(), 'Курбан-айт',                            '2026-06-07', false),
         (gen_random_uuid(), 'День Столицы',                          '2026-07-06', true),
         (gen_random_uuid(), 'День Конституции',                      '2026-08-30', true),
         (gen_random_uuid(), 'День Республики',                       '2026-10-25', true),
         (gen_random_uuid(), 'День Первого Президента',               '2026-12-01', true),
         (gen_random_uuid(), 'День Независимости',                    '2026-12-16', true),
         (gen_random_uuid(), 'День Независимости',                    '2026-12-17', true);


-- 12. SEED: WORK SCHEDULE + COMPANY SETTINGS

INSERT INTO work_schedules (id, name, is_default) VALUES
(gen_random_uuid(), 'Standard (09:00-18:00)', true);

INSERT INTO company_settings (id, key, value, description, category) VALUES
         (gen_random_uuid(), 'company.name',             'ТОО Компания',   'Organization name',           'GENERAL'),
         (gen_random_uuid(), 'company.bin',              '000000000000',    'BIN (БИН)',                   'GENERAL'),
         (gen_random_uuid(), 'company.address',          '',                'Legal address',               'GENERAL'),
         (gen_random_uuid(), 'company.kbe',              '17',              'КБЕ code for bank transfers', 'GENERAL'),
         (gen_random_uuid(), 'payroll.auto_approve',     'false',           'Auto-approve payslips',       'PAYROLL'),
         (gen_random_uuid(), 'attendance.late_threshold', '10',             'Minutes after start = late',  'ATTENDANCE'),
         (gen_random_uuid(), 'attendance.work_start',    '09:00',           'Default work start time',     'ATTENDANCE'),
         (gen_random_uuid(), 'attendance.work_end',      '18:00',           'Default work end time',       'ATTENDANCE'),
         (gen_random_uuid(), 'leave.carryover_pct',      '50',              'Max % annual leave carryover','LEAVE'),
         (gen_random_uuid(), 'notification.email_enabled','false',          'Send email notifications',    'NOTIFICATION'),
         (gen_random_uuid(), 'integration.1c_enabled',   'false',           'Enable 1C sync',              'INTEGRATION'),
         (gen_random_uuid(), 'integration.1c_base_url',  '',                '1C HTTP Service URL',         'INTEGRATION');