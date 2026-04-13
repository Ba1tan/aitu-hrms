# HRMS Enterprise Architecture — Complete Specification

**System:** Human Resource Management System with Automated Payroll & AI  
**Target:** Kazakhstan SMEs (10–500 employees)  
**Team:** Nursultan Bukenbayev, Askar Seralinov, Nurbol Sembayev  
**Version:** 2.0 — Enterprise Microservices Architecture  

---

## 1. System Overview

### 1.1 What This System Does

A fully functioning HR platform for Kazakhstan businesses that handles the entire employee lifecycle — from hiring to termination — with automated payroll compliant with the 2026 Kazakhstan Tax Code, AI-powered fraud detection, anomaly flagging, attrition prediction, and deep integration with 1C:Enterprise for government reporting.

### 1.2 Core Innovations

1. **Automated Payroll Engine** — 10-step Kazakhstan tax calculation (ИПН, ОПВ, ВОСМС, СО, СН, ОПВР) with proration, disability deductions, and resident/non-resident handling
2. **AI Payroll Anomaly Detection** — Isolation Forest catches salary spikes, ghost employees, and suspicious allowances before payslips are approved
3. **AI Attendance Fraud Detection** — Isolation Forest on check-in patterns detects buddy-punching, impossible locations, and device spoofing
4. **AI Attrition Prediction** — XGBoost predicts flight-risk employees with actionable retention recommendations
5. **1C:Enterprise Integration** — Automated sync of payroll data for Form 200.00 generation and accounting entries

### 1.3 Users & Roles

| Role | Users (%) | What They Do |
|------|-----------|-------------|
| SUPER_ADMIN | 1–2% | Full system access, user management, settings, role configuration |
| DIRECTOR | 2–3% | Executive dashboards, company-wide salary/headcount analytics, read-only |
| HR_MANAGER | 5–8% | Employee lifecycle, payroll processing/approval, leave management, all reports |
| HR_SPECIALIST | 5–10% | Employee data entry, attendance management, leave processing (no payroll) |
| ACCOUNTANT | 3–5% | View/verify payroll, mark as paid, Form 200.00, financial reports |
| MANAGER | 5–10% | Approve team leave, view team attendance/reports, org structure |
| TEAM_LEAD | 5–10% | View team attendance, approve team leave (narrower scope than MANAGER) |
| EMPLOYEE | 60–75% | Self-service: payslips, leave requests, attendance check-in, profile, notifications |

### 1.4 Permission-Based RBAC

Roles map to granular permissions. Admins can customize role → permission mappings without code changes.

**Permission Categories (32 total):**

```
EMPLOYEE_VIEW_OWN          EMPLOYEE_VIEW_TEAM         EMPLOYEE_VIEW_ALL
EMPLOYEE_CREATE            EMPLOYEE_UPDATE            EMPLOYEE_DELETE
EMPLOYEE_SALARY_VIEW       EMPLOYEE_SALARY_CHANGE
PAYROLL_VIEW               PAYROLL_PROCESS            PAYROLL_APPROVE
PAYROLL_PAY                PAYSLIP_VIEW_OWN           PAYSLIP_ADJUST
LEAVE_REQUEST_OWN          LEAVE_APPROVE_TEAM         LEAVE_APPROVE_ALL
LEAVE_BALANCE_MANAGE
ATTENDANCE_CHECKIN         ATTENDANCE_VIEW_TEAM       ATTENDANCE_VIEW_ALL
ATTENDANCE_MANAGE
REPORT_PAYROLL             REPORT_ATTENDANCE          REPORT_LEAVE
REPORT_FORM200             REPORT_EXECUTIVE
SYSTEM_SETTINGS            SYSTEM_USERS               SYSTEM_AUDIT
SYSTEM_ROLES               AI_DASHBOARD
```

**Default Role → Permission Mapping:**

| Permission | SUPER_ADMIN | DIRECTOR | HR_MANAGER | HR_SPECIALIST | ACCOUNTANT | MANAGER | TEAM_LEAD | EMPLOYEE |
|-----------|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| EMPLOYEE_VIEW_OWN | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| EMPLOYEE_VIEW_TEAM | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ | — |
| EMPLOYEE_VIEW_ALL | ✓ | ✓ | ✓ | ✓ | — | — | — | — |
| EMPLOYEE_CREATE | ✓ | — | ✓ | ✓ | — | — | — | — |
| EMPLOYEE_UPDATE | ✓ | — | ✓ | ✓ | — | — | — | — |
| EMPLOYEE_DELETE | ✓ | — | ✓ | — | — | — | — | — |
| EMPLOYEE_SALARY_VIEW | ✓ | ✓ | ✓ | — | ✓ | — | — | — |
| EMPLOYEE_SALARY_CHANGE | ✓ | — | ✓ | — | — | — | — | — |
| PAYROLL_VIEW | ✓ | ✓ | ✓ | — | ✓ | — | — | — |
| PAYROLL_PROCESS | ✓ | — | ✓ | — | — | — | — | — |
| PAYROLL_APPROVE | ✓ | — | ✓ | — | — | — | — | — |
| PAYROLL_PAY | ✓ | — | — | — | ✓ | — | — | — |
| PAYSLIP_VIEW_OWN | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| PAYSLIP_ADJUST | ✓ | — | ✓ | — | ✓ | — | — | — |
| LEAVE_REQUEST_OWN | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| LEAVE_APPROVE_TEAM | ✓ | — | ✓ | ✓ | — | ✓ | ✓ | — |
| LEAVE_APPROVE_ALL | ✓ | — | ✓ | ✓ | — | — | — | — |
| LEAVE_BALANCE_MANAGE | ✓ | — | ✓ | — | — | — | — | — |
| ATTENDANCE_CHECKIN | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| ATTENDANCE_VIEW_TEAM | ✓ | ✓ | ✓ | ✓ | — | ✓ | ✓ | — |
| ATTENDANCE_VIEW_ALL | ✓ | ✓ | ✓ | ✓ | — | — | — | — |
| ATTENDANCE_MANAGE | ✓ | — | ✓ | ✓ | — | — | — | — |
| REPORT_PAYROLL | ✓ | ✓ | ✓ | — | ✓ | — | — | — |
| REPORT_ATTENDANCE | ✓ | ✓ | ✓ | ✓ | — | ✓ | — | — |
| REPORT_LEAVE | ✓ | ✓ | ✓ | ✓ | — | ✓ | — | — |
| REPORT_FORM200 | ✓ | — | ✓ | — | ✓ | — | — | — |
| REPORT_EXECUTIVE | ✓ | ✓ | — | — | — | — | — | — |
| SYSTEM_SETTINGS | ✓ | — | — | — | — | — | — | — |
| SYSTEM_USERS | ✓ | — | — | — | — | — | — | — |
| SYSTEM_AUDIT | ✓ | — | ✓ | — | ✓ | — | — | — |
| SYSTEM_ROLES | ✓ | — | — | — | — | — | — | — |
| AI_DASHBOARD | ✓ | ✓ | ✓ | — | — | — | — | — |

---

## 2. Microservices Architecture

### 2.1 Service Decomposition

```
                            ┌──────────────┐
                            │   Clients    │
                            │ Web / Mobile │
                            └──────┬───────┘
                                   │
                            ┌──────▼───────┐
                     ┌──────┤ API Gateway  ├──────┐
                     │      │    :8080     │      │
                     │      └──────────────┘      │
          ┌──────────┼────────────┼───────────────┼──────────┐
          │          │            │               │          │
   ┌──────▼──┐ ┌────▼─────┐ ┌───▼────┐ ┌───────▼──┐ ┌────▼─────┐
   │  User   │ │ Employee │ │ Leave  │ │Attendance│ │ Payroll  │
   │ :8081   │ │  :8082   │ │ :8084  │ │  :8083   │ │  :8085   │
   └────┬────┘ └────┬─────┘ └───┬────┘ └────┬─────┘ └────┬─────┘
        │           │           │            │            │
        │      ┌────▼─────┐    │       ┌────▼─────┐ ┌───▼──────┐
        │      │Reporting │    │       │  AI/ML   │ │Integrat° │
        │      │  :8087   │    │       │  :8086   │ │Hub :8089 │
        │      └──────────┘    │       └──────────┘ └──────────┘
        │                      │
   ┌────▼──────────────────────▼──────┐
   │        Notification :8088        │
   └──────────────────────────────────┘
        │           │           │
   ┌────▼────┐ ┌───▼────┐ ┌───▼──────┐
   │PostgreSQL│ │ Redis  │ │ RabbitMQ │
   │  :5432  │ │ :6379  │ │  :5672   │
   └─────────┘ └────────┘ └──────────┘
```

### 2.2 Service Registry

| # | Service | Port | DB Schema | Tech | Responsibility |
|---|---------|------|-----------|------|----------------|
| 1 | **api-gateway** | 8080 | — | Spring Cloud Gateway | JWT validation, routing, rate limiting, CORS |
| 2 | **user-service** | 8081 | hrms_user | Spring Boot | Auth, RBAC, user CRUD, permissions, sessions |
| 3 | **employee-service** | 8082 | hrms_employee | Spring Boot | Employee CRUD, departments, positions, org chart, documents, salary history |
| 4 | **attendance-service** | 8083 | hrms_attendance | Spring Boot | Check-in/out, holidays, schedules, biometric WebSocket |
| 5 | **leave-service** | 8084 | hrms_leave | Spring Boot | Leave types, requests, approvals, balances, calendar |
| 6 | **payroll-service** | 8085 | hrms_payroll | Spring Boot | KZ tax calculator, Spring Batch, payslip generation, additions |
| 7 | **ai-ml-service** | 8086 | — (stateless) | Python FastAPI | Isolation Forest anomaly/fraud, XGBoost attrition, forecasting |
| 8 | **reporting-service** | 8087 | hrms_reporting | Spring Boot | XLSX/PDF generation, Form 200.00, dashboards |
| 9 | **notification-service** | 8088 | hrms_notification | Spring Boot | DB notifications, email (SMTP), push (FCM) |
| 10 | **integration-hub** | 8089 | hrms_integration | Spring Boot | 1C:Enterprise OData sync, bank file generation |

### 2.3 Communication Patterns

**Synchronous (REST via Feign):**
- Client → Gateway → Service (all user-facing requests)
- Service → Service (immediate data lookups: employee details, balance checks)

**Asynchronous (RabbitMQ Events):**
- Event-driven workflows where response is not needed immediately
- Decouples services — failure in one doesn't cascade

**Real-time (WebSocket):**
- Biometric devices → Attendance Service (fingerprint check-in stream)
- Future: live dashboard updates

### 2.4 Event Catalog

| Event | Publisher | Consumers |
|-------|-----------|-----------|
| `EmployeeCreatedEvent` | employee-service | user-service, payroll-service, leave-service, notification-service |
| `EmployeeTerminatedEvent` | employee-service | user-service, payroll-service, leave-service, notification-service |
| `SalaryChangedEvent` | employee-service | payroll-service, notification-service |
| `LeaveRequestCreatedEvent` | leave-service | notification-service |
| `LeaveApprovedEvent` | leave-service | notification-service, attendance-service |
| `LeaveRejectedEvent` | leave-service | notification-service |
| `AttendanceRecordedEvent` | attendance-service | notification-service (dashboard update) |
| `FraudAttemptDetectedEvent` | attendance-service | notification-service |
| `PayrollJobStartedEvent` | payroll-service | notification-service |
| `PayrollJobCompletedEvent` | payroll-service | notification-service, integration-hub, reporting-service |
| `PayrollAnomalyDetectedEvent` | payroll-service | notification-service |
| `PayrollPeriodApprovedEvent` | payroll-service | integration-hub |
| `IntegrationSyncCompletedEvent` | integration-hub | notification-service |
| `IntegrationSyncFailedEvent` | integration-hub | notification-service |
| `UserAccountCreatedEvent` | user-service | notification-service |
| `PasswordResetRequestedEvent` | user-service | notification-service |

---

## 3. Database Architecture

### 3.1 Schema per Service

All services share one PostgreSQL 16 cluster but each owns its own schema with full isolation.

```sql
CREATE SCHEMA hrms_user;
CREATE SCHEMA hrms_employee;
CREATE SCHEMA hrms_attendance;
CREATE SCHEMA hrms_leave;
CREATE SCHEMA hrms_payroll;
CREATE SCHEMA hrms_notification;
CREATE SCHEMA hrms_reporting;    -- materialized views only
CREATE SCHEMA hrms_integration;
```

### 3.2 Complete Table Definitions

#### Schema: hrms_user (user-service)

```sql
-- Users (authentication & authorization)
CREATE TABLE hrms_user.users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    role                VARCHAR(20) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked  BOOLEAN NOT NULL DEFAULT TRUE,
    employee_id         UUID,             -- FK enforced at app level (cross-schema)
    require_password_change BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at       TIMESTAMP,
    last_login_ip       VARCHAR(45),
    failed_login_count  INTEGER NOT NULL DEFAULT 0,
    locked_until        TIMESTAMP,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

-- Permissions
CREATE TABLE hrms_user.permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    module      VARCHAR(50) NOT NULL
);

-- Role ↔ Permission mapping
CREATE TABLE hrms_user.role_permissions (
    role          VARCHAR(20) NOT NULL,
    permission_id UUID NOT NULL REFERENCES hrms_user.permissions(id),
    PRIMARY KEY (role, permission_id)
);

-- Audit log (every sensitive action)
CREATE TABLE hrms_user.audit_logs (
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
CREATE INDEX idx_audit_entity ON hrms_user.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_user ON hrms_user.audit_logs(user_id);
CREATE INDEX idx_audit_date ON hrms_user.audit_logs(created_at);
```

#### Schema: hrms_employee (employee-service)

```sql
-- Departments
CREATE TABLE hrms_employee.departments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    cost_center VARCHAR(50),
    manager_id  UUID,
    parent_id   UUID REFERENCES hrms_employee.departments(id),
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
);

-- Positions
CREATE TABLE hrms_employee.positions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         VARCHAR(150) NOT NULL,
    description   TEXT,
    min_salary    NUMERIC(15,2),
    max_salary    NUMERIC(15,2),
    department_id UUID REFERENCES hrms_employee.departments(id),
    is_deleted    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP,
    created_by    VARCHAR(255),
    updated_by    VARCHAR(255)
);

-- Employees
CREATE TABLE hrms_employee.employees (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_number   VARCHAR(20) NOT NULL UNIQUE,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    middle_name       VARCHAR(100),
    date_of_birth     DATE,
    iin               VARCHAR(12) UNIQUE,
    email             VARCHAR(255) NOT NULL UNIQUE,
    phone             VARCHAR(20),
    hire_date         DATE NOT NULL,
    termination_date  DATE,
    termination_reason VARCHAR(255),
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','ON_LEAVE','TERMINATED','PROBATION','SUSPENDED')),
    employment_type   VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME'
                        CHECK (employment_type IN ('FULL_TIME','PART_TIME','CONTRACT','INTERN')),
    base_salary       NUMERIC(15,2) NOT NULL,
    department_id     UUID REFERENCES hrms_employee.departments(id),
    position_id       UUID REFERENCES hrms_employee.positions(id),
    manager_id        UUID REFERENCES hrms_employee.employees(id),
    bank_account      VARCHAR(34),
    bank_name         VARCHAR(150),
    profile_photo_url TEXT,
    is_resident       BOOLEAN NOT NULL DEFAULT TRUE,
    has_disability    BOOLEAN NOT NULL DEFAULT FALSE,
    disability_group  INTEGER CHECK (disability_group IN (1, 2, 3)),
    is_pensioner      BOOLEAN NOT NULL DEFAULT FALSE,
    address           TEXT,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255)
);
ALTER TABLE hrms_employee.departments ADD CONSTRAINT fk_dept_manager
    FOREIGN KEY (manager_id) REFERENCES hrms_employee.employees(id);

-- Salary history
CREATE TABLE hrms_employee.salary_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES hrms_employee.employees(id),
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
CREATE TABLE hrms_employee.employee_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES hrms_employee.employees(id),
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
CREATE TABLE hrms_employee.emergency_contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES hrms_employee.employees(id),
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

CREATE INDEX idx_employees_dept ON hrms_employee.employees(department_id);
CREATE INDEX idx_employees_status ON hrms_employee.employees(status) WHERE is_deleted = false;
CREATE INDEX idx_employees_manager ON hrms_employee.employees(manager_id);
CREATE INDEX idx_employees_name ON hrms_employee.employees(lower(last_name), lower(first_name)) WHERE is_deleted = false;
CREATE INDEX idx_salary_history_emp ON hrms_employee.salary_history(employee_id);
CREATE INDEX idx_documents_emp ON hrms_employee.employee_documents(employee_id);
```

#### Schema: hrms_attendance (attendance-service)

```sql
-- Work schedules
CREATE TABLE hrms_attendance.work_schedules (
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

-- Holidays (Kazakhstan public holidays)
CREATE TABLE hrms_attendance.holidays (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    date        DATE NOT NULL,
    year        INTEGER NOT NULL,
    is_annual   BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    is_deleted  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    UNIQUE(date, year)
);

-- Attendance records
CREATE TABLE hrms_attendance.attendance_records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL,
    work_date       DATE NOT NULL,
    check_in        TIMESTAMP,
    check_out       TIMESTAMP,
    worked_hours    NUMERIC(5,2),
    overtime_hours  NUMERIC(5,2) DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PRESENT'
                      CHECK (status IN ('PRESENT','ABSENT','LATE','HALF_DAY','HOLIDAY','WEEKEND','ON_LEAVE')),
    check_in_method VARCHAR(20) DEFAULT 'MANUAL'
                      CHECK (check_in_method IN ('MANUAL','WEB','MOBILE','BIOMETRIC')),
    device_id       VARCHAR(50),
    location_lat    NUMERIC(10,7),
    location_lng    NUMERIC(10,7),
    fraud_score     NUMERIC(5,4),
    fraud_flags     TEXT,
    note            TEXT,
    approved_by     UUID,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    UNIQUE (employee_id, work_date)
);

-- Biometric data (fingerprint hashes for verification)
CREATE TABLE hrms_attendance.biometric_data (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL UNIQUE,
    fingerprint_hash VARCHAR(128) NOT NULL,
    enrolled_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    enrolled_by     UUID,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- Failed biometric attempts (security log)
CREATE TABLE hrms_attendance.biometric_attempts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fingerprint_hash VARCHAR(128) NOT NULL,
    device_id       VARCHAR(50),
    result          VARCHAR(20) NOT NULL CHECK (result IN ('SUCCESS','FAILED','BLOCKED')),
    fraud_score     NUMERIC(5,4),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attendance_emp_date ON hrms_attendance.attendance_records(employee_id, work_date);
CREATE INDEX idx_attendance_date ON hrms_attendance.attendance_records(work_date) WHERE is_deleted = false;
CREATE INDEX idx_holidays_date ON hrms_attendance.holidays(date);
```

#### Schema: hrms_leave (leave-service)

```sql
-- Leave types
CREATE TABLE hrms_leave.leave_types (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL UNIQUE,
    days_allowed    INTEGER NOT NULL,
    is_paid         BOOLEAN NOT NULL DEFAULT TRUE,
    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
    max_consecutive INTEGER,
    carryover_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    carryover_max_days INTEGER DEFAULT 0,
    description     TEXT,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

-- Leave requests
CREATE TABLE hrms_leave.leave_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL,
    leave_type_id   UUID NOT NULL REFERENCES hrms_leave.leave_types(id),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    days_requested  INTEGER NOT NULL,
    reason          TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    reviewed_by     UUID,
    reviewed_at     TIMESTAMP,
    review_comment  TEXT,
    cancelled_at    TIMESTAMP,
    cancel_reason   TEXT,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

-- Leave balances
CREATE TABLE hrms_leave.leave_balances (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL,
    leave_type_id   UUID NOT NULL REFERENCES hrms_leave.leave_types(id),
    year            INTEGER NOT NULL,
    entitled_days   INTEGER NOT NULL,
    used_days       INTEGER NOT NULL DEFAULT 0,
    carried_over    INTEGER NOT NULL DEFAULT 0,
    adjusted_days   INTEGER NOT NULL DEFAULT 0,
    remaining_days  INTEGER GENERATED ALWAYS AS (entitled_days + carried_over + adjusted_days - used_days) STORED,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    UNIQUE (employee_id, leave_type_id, year)
);

-- Balance adjustments (audit trail for manual changes)
CREATE TABLE hrms_leave.balance_adjustments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    balance_id      UUID NOT NULL REFERENCES hrms_leave.leave_balances(id),
    adjustment      INTEGER NOT NULL,
    reason          TEXT NOT NULL,
    adjusted_by     UUID NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leave_req_emp ON hrms_leave.leave_requests(employee_id);
CREATE INDEX idx_leave_req_status ON hrms_leave.leave_requests(status) WHERE status = 'PENDING' AND is_deleted = false;
CREATE INDEX idx_leave_req_dates ON hrms_leave.leave_requests(start_date, end_date) WHERE is_deleted = false;
CREATE INDEX idx_leave_bal_emp ON hrms_leave.leave_balances(employee_id, year);
```

#### Schema: hrms_payroll (payroll-service)

```sql
-- Payroll periods
CREATE TABLE hrms_payroll.payroll_periods (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year            INTEGER NOT NULL,
    month           INTEGER NOT NULL CHECK (month BETWEEN 1 AND 12),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    working_days    INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                      CHECK (status IN ('DRAFT','PROCESSING','COMPLETED','APPROVED','PAID','LOCKED')),
    processed_by    UUID,
    processed_at    TIMESTAMP,
    approved_by     UUID,
    approved_at     TIMESTAMP,
    batch_job_id    BIGINT,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    UNIQUE (year, month)
);

-- Payslips
CREATE TABLE hrms_payroll.payslips (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id           UUID NOT NULL REFERENCES hrms_payroll.payroll_periods(id),
    employee_id         UUID NOT NULL,
    employee_iin        VARCHAR(12),
    employee_name       VARCHAR(300),
    worked_days         INTEGER NOT NULL,
    total_working_days  INTEGER NOT NULL,
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
    -- Employer obligations
    so_amount           NUMERIC(15,2) NOT NULL,
    sn_amount           NUMERIC(15,2) NOT NULL,
    opvr_amount         NUMERIC(15,2) NOT NULL DEFAULT 0,
    -- Metadata
    mrp_used            INTEGER NOT NULL,
    is_resident         BOOLEAN NOT NULL DEFAULT TRUE,
    has_disability      BOOLEAN NOT NULL DEFAULT FALSE,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                          CHECK (status IN ('DRAFT','FLAGGED','APPROVED','PAID')),
    -- AI anomaly detection
    anomaly_score       NUMERIC(5,4),
    anomaly_flags       TEXT,
    ai_reviewed         BOOLEAN NOT NULL DEFAULT FALSE,
    ai_reviewed_by      UUID,
    ai_reviewed_at      TIMESTAMP,
    -- PDF
    pdf_url             TEXT,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    UNIQUE (period_id, employee_id)
);

-- Payroll additions (bonuses and deductions)
CREATE TABLE hrms_payroll.payroll_additions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL,
    period_id       UUID NOT NULL REFERENCES hrms_payroll.payroll_periods(id),
    type            VARCHAR(20) NOT NULL CHECK (type IN ('BONUS', 'DEDUCTION')),
    category        VARCHAR(50) NOT NULL CHECK (category IN (
        'MEAL_ALLOWANCE','TRANSPORT','OVERTIME','BONUS_PERFORMANCE','BONUS_HOLIDAY',
        'FINE','ADVANCE_REPAYMENT','TAX_ADJUSTMENT','INSURANCE','OTHER')),
    description     VARCHAR(255),
    amount          NUMERIC(15,2) NOT NULL,
    is_taxable      BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

-- Salary advances
CREATE TABLE hrms_payroll.salary_advances (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL,
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

CREATE INDEX idx_payslips_period ON hrms_payroll.payslips(period_id);
CREATE INDEX idx_payslips_emp ON hrms_payroll.payslips(employee_id);
CREATE INDEX idx_payslips_status ON hrms_payroll.payslips(status) WHERE is_deleted = false;
CREATE INDEX idx_payslips_flagged ON hrms_payroll.payslips(period_id) WHERE status = 'FLAGGED';
CREATE INDEX idx_additions_period ON hrms_payroll.payroll_additions(period_id, employee_id);
```

#### Schema: hrms_notification (notification-service)

```sql
CREATE TABLE hrms_notification.notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
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
CREATE INDEX idx_notif_user ON hrms_notification.notifications(user_id);
CREATE INDEX idx_notif_unread ON hrms_notification.notifications(user_id, is_read) WHERE is_read = FALSE;
```

#### Schema: hrms_integration (integration-hub)

```sql
CREATE TABLE hrms_integration.sync_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id       UUID NOT NULL,
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
CREATE INDEX idx_sync_period ON hrms_integration.sync_jobs(period_id);
CREATE INDEX idx_sync_status ON hrms_integration.sync_jobs(status);

-- Company settings (used by integration-hub for BIN, company name, etc.)
CREATE TABLE hrms_integration.company_settings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(100) NOT NULL UNIQUE,
    value       TEXT NOT NULL,
    description TEXT,
    category    VARCHAR(50) NOT NULL,
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(255)
);
```

---

## 4. Complete API Specification

### 4.1 API Gateway (Port 8080)

All client requests go through the gateway. Base path: `/api`.

### 4.2 User Service — 14 endpoints

```
# Authentication (Public — no JWT required)
POST   /v1/auth/login                                  # {email, password} → tokens + user
POST   /v1/auth/refresh                                # {refreshToken} → new tokens
POST   /v1/auth/forgot-password                        # {email} → sends reset email
POST   /v1/auth/reset-password                         # {token, newPassword}

# Authenticated
POST   /v1/auth/logout                                 # Blacklist current token
POST   /v1/auth/change-password                        # {currentPassword, newPassword}
GET    /v1/auth/me                                     # Current user profile + employee data
PUT    /v1/auth/me                                     # Update own profile {firstName, lastName, phone}

# User Management (SYSTEM_USERS)
GET    /v1/users                                       # List all users (paginated, search)
GET    /v1/users/{id}                                  # User detail
POST   /v1/users                                       # Create user {email, role, employeeId?}
PUT    /v1/users/{id}                                  # Update {role, enabled, locked}
DELETE /v1/users/{id}                                  # Soft delete
PUT    /v1/users/{id}/link-employee                    # Link to employee {employeeId}
```

### 4.3 Employee Service — 25 endpoints

```
# Employees (EMPLOYEE_*)
POST   /v1/employees                                   # Create employee (auto-generates number)
GET    /v1/employees                                   # List (paginated, search, filters: dept/status/type)
GET    /v1/employees/{id}                              # Detail (includes dept, position, manager)
PUT    /v1/employees/{id}                              # Update employee data
PATCH  /v1/employees/{id}/status                       # Change status {status, reason}
DELETE /v1/employees/{id}                              # Soft delete

# Onboarding & Offboarding
POST   /v1/employees/{id}/create-account               # Create user account for employee
POST   /v1/employees/{id}/terminate                    # Terminate {terminationDate, reason}

# Salary History (EMPLOYEE_SALARY_*)
GET    /v1/employees/{id}/salary-history               # All salary changes
POST   /v1/employees/{id}/salary-change                # Record change {newSalary, effectiveDate, reason}

# Documents (EMPLOYEE_UPDATE)
GET    /v1/employees/{id}/documents                    # List documents
POST   /v1/employees/{id}/documents                    # Upload (multipart/form-data)
GET    /v1/employees/{id}/documents/{docId}/download   # Download file
DELETE /v1/employees/{id}/documents/{docId}            # Delete document

# Emergency Contacts
GET    /v1/employees/{id}/emergency-contacts
POST   /v1/employees/{id}/emergency-contacts
PUT    /v1/employees/{id}/emergency-contacts/{cId}
DELETE /v1/employees/{id}/emergency-contacts/{cId}

# Organization
GET    /v1/employees/org-chart                         # Hierarchical tree
GET    /v1/employees/org-chart/{id}                    # Subtree from employee
POST   /v1/employees/import                            # Bulk import XLSX
GET    /v1/employees/export                            # Bulk export XLSX

# Departments (part of employee-service)
POST   /v1/departments                                 # Create
GET    /v1/departments                                 # List all
GET    /v1/departments/{id}                            # Detail
PUT    /v1/departments/{id}                            # Update
DELETE /v1/departments/{id}                            # Soft delete

# Positions
POST   /v1/positions                                   # Create
GET    /v1/positions                                   # List (filter by dept)
GET    /v1/positions/{id}                              # Detail
PUT    /v1/positions/{id}                              # Update
DELETE /v1/positions/{id}                              # Soft delete
```

### 4.4 Attendance Service — 18 endpoints

```
# Check-in/out (ATTENDANCE_CHECKIN)
POST   /v1/attendance/check-in                         # {employeeId?, method?, location?}
POST   /v1/attendance/check-out                        # Same
GET    /v1/attendance/today                            # My status today

# Records
GET    /v1/attendance/records                          # Own records (?from=&to=, paginated)
GET    /v1/attendance/records/employee/{id}            # Specific employee (ATTENDANCE_VIEW_*)
GET    /v1/attendance/records/department/{id}          # Department view (?date=)
GET    /v1/attendance/records/daily                    # Company-wide today (?date=)

# Manual Management (ATTENDANCE_MANAGE)
POST   /v1/attendance/records                          # Manual entry {employeeId, workDate, checkIn, checkOut, status}
PUT    /v1/attendance/records/{id}                     # Correct record
POST   /v1/attendance/records/bulk-absent              # Mark no-shows {date}

# Summaries
GET    /v1/attendance/summary/employee/{id}            # ?year=&month= → present/late/absent/hours
GET    /v1/attendance/summary/department/{id}          # Same, aggregated
GET    /v1/attendance/summary/company                  # Same, company-wide

# Holidays (ATTENDANCE_MANAGE)
GET    /v1/attendance/holidays                         # ?year= → list
POST   /v1/attendance/holidays                        # Create
PUT    /v1/attendance/holidays/{id}                    # Update
DELETE /v1/attendance/holidays/{id}                    # Delete

# Work Schedules (ATTENDANCE_MANAGE)
GET    /v1/attendance/schedules
POST   /v1/attendance/schedules
PUT    /v1/attendance/schedules/{id}

# Biometric (Phase 5 — WebSocket)
# WS   /ws/biometric                                  # Real-time fingerprint check-in stream
POST   /v1/attendance/biometric/enroll                 # {employeeId, fingerprintHash}
```

### 4.5 Leave Service — 19 endpoints

```
# Leave Types (LEAVE_BALANCE_MANAGE)
GET    /v1/leave/types                                 # List all
POST   /v1/leave/types                                 # Create
PUT    /v1/leave/types/{id}                            # Update
DELETE /v1/leave/types/{id}                            # Soft delete

# Leave Requests
POST   /v1/leave/requests                              # Submit {leaveTypeId, startDate, endDate, reason}
GET    /v1/leave/requests                              # Own requests (paginated)
GET    /v1/leave/requests/{id}                         # Detail
PUT    /v1/leave/requests/{id}/approve                 # Approve (LEAVE_APPROVE_*)
PUT    /v1/leave/requests/{id}/reject                  # Reject {comment}
PUT    /v1/leave/requests/{id}/cancel                  # Cancel (with balance reversal if was approved)
GET    /v1/leave/requests/pending                      # Pending for my approval
GET    /v1/leave/requests/team                         # My team's requests
GET    /v1/leave/requests/all                          # All (HR) — paginated with filters

# Leave Balances
GET    /v1/leave/balances                              # Own balances (current year)
GET    /v1/leave/balances/employee/{id}                # Specific employee (?year=)
GET    /v1/leave/balances/department/{id}              # Department summary
POST   /v1/leave/balances/initialize                   # Create balances for year {year}
PUT    /v1/leave/balances/{id}/adjust                  # Manual adjustment {days, reason}
POST   /v1/leave/balances/carryover                    # Roll unused into next year {fromYear}

# Leave Calendar
GET    /v1/leave/calendar                              # ?month=&year=&departmentId= → who's on leave
```

### 4.6 Payroll Service — 23 endpoints

```
# Payroll Periods
POST   /v1/payroll/periods                             # Create {year, month, workingDays}
GET    /v1/payroll/periods                             # List (paginated)
GET    /v1/payroll/periods/{id}                        # Detail with summary
POST   /v1/payroll/periods/{id}/generate               # Generate payslips (sync MVP / async with Batch)
POST   /v1/payroll/periods/{id}/approve                # Approve period
POST   /v1/payroll/periods/{id}/mark-paid              # Mark as paid
POST   /v1/payroll/periods/{id}/lock                   # Lock period (immutable)

# Batch Job Status (Phase 5 — Spring Batch)
GET    /v1/payroll/jobs/{jobId}/status                  # Check async processing status

# Payslips
GET    /v1/payroll/periods/{id}/payslips               # All payslips for period (paginated)
GET    /v1/payroll/payslips/{id}                       # Single payslip detail
PATCH  /v1/payroll/payslips/{id}/adjust                # Adjust {allowances, deductions, workedDays}
POST   /v1/payroll/payslips/{id}/recalculate           # Recalculate after adjustment
GET    /v1/payroll/payslips/{id}/pdf                   # Download payslip PDF
POST   /v1/payroll/payslips/{id}/approve-flagged       # Approve AI-flagged payslip after review

# Employee Self-Service (PAYSLIP_VIEW_OWN)
GET    /v1/payroll/my-payslips                         # Own payslips (paginated)
GET    /v1/payroll/my-payslips/period/{id}             # Own payslip for specific period
GET    /v1/payroll/my-payslips/{id}/pdf                # Download own payslip PDF

# Year-to-Date
GET    /v1/payroll/ytd/employee/{id}                   # ?year= → cumulative tax totals

# Payroll Additions (Bonuses/Deductions)
GET    /v1/payroll/additions                           # ?periodId=&employeeId=
POST   /v1/payroll/additions                           # Create {employeeId, periodId, type, category, amount}
PUT    /v1/payroll/additions/{id}                      # Update
DELETE /v1/payroll/additions/{id}                      # Delete
POST   /v1/payroll/additions/bulk                      # Bulk create for all/selected employees
```

### 4.7 AI/ML Service — 8 endpoints

```
# Payroll Anomaly Detection
POST   /v1/ai/payroll/detect                           # Single payslip anomaly check
POST   /v1/ai/payroll/detect/batch                     # Batch check (array of payslips)

# Attendance Fraud Detection
POST   /v1/ai/attendance/fraud-detect                  # Single check-in fraud analysis

# Attrition Prediction
GET    /v1/ai/attrition/risk                           # All employees (?departmentId=)
GET    /v1/ai/attrition/risk/employee/{id}             # Individual employee risk
GET    /v1/ai/attrition/dashboard                      # Company-wide summary

# Payroll Forecasting
GET    /v1/ai/payroll/forecast                         # ?months=3 → predicted payroll costs

# Health
GET    /v1/ai/health                                   # Model versions, load status
```

### 4.8 Reporting Service — 12 endpoints

```
# Payroll Reports (REPORT_PAYROLL)
GET    /v1/reports/payroll-summary                     # ?periodId= → XLSX
GET    /v1/reports/payroll-summary/pdf                 # ?periodId= → PDF
GET    /v1/reports/form200                             # ?year=&quarter= → XLSX (quarterly tax)
GET    /v1/reports/salary-breakdown                    # ?departmentId= → XLSX

# Attendance Reports (REPORT_ATTENDANCE)
GET    /v1/reports/attendance-monthly                  # ?year=&month= → XLSX (grid view)
GET    /v1/reports/attendance-summary                  # ?year=&month= → XLSX (stats)

# Leave Reports (REPORT_LEAVE)
GET    /v1/reports/leave-balances                      # ?year= → XLSX

# HR Reports
GET    /v1/reports/employee-directory                  # → XLSX
GET    /v1/reports/turnover                            # ?year= → XLSX (hires/terms per month)
GET    /v1/reports/headcount                           # ?from=&to= → XLSX

# Executive (REPORT_EXECUTIVE)
GET    /v1/reports/executive-summary                   # ?year=&month= → PDF (all-in-one)

# AI Report (AI_DASHBOARD)
GET    /v1/reports/ai-insights                         # → PDF (anomalies, attrition risks, forecasts)
```

### 4.9 Notification Service — 5 endpoints

```
GET    /v1/notifications                               # My notifications (paginated, newest first)
GET    /v1/notifications/unread-count                  # Integer count (for badge)
PUT    /v1/notifications/{id}/read                     # Mark one as read
PUT    /v1/notifications/read-all                      # Mark all as read
DELETE /v1/notifications/{id}                          # Soft delete
```

### 4.10 Integration Hub — 7 endpoints

```
# 1C Sync (SYSTEM_SETTINGS or PAYROLL_APPROVE)
POST   /v1/integration/sync/{periodId}                 # Manual trigger 1C sync
GET    /v1/integration/sync/status/{jobId}             # Check sync status
GET    /v1/integration/sync/history                    # List all sync jobs (paginated)
POST   /v1/integration/retry/{jobId}                   # Retry failed sync

# Bank File
GET    /v1/integration/bank-file/{periodId}            # Download bank payment file

# Settings (SYSTEM_SETTINGS)
GET    /v1/settings                                    # All company settings
PUT    /v1/settings/{key}                              # Update setting value
```

### 4.11 Dashboard — 1 endpoint

```
GET    /v1/dashboard/stats                             # Role-aware dashboard data
```

---

## 5. Endpoint Summary

| Service | Endpoints |
|---------|-----------|
| User Service | 14 |
| Employee Service | 25 (+10 dept/position) = 35 |
| Attendance Service | 18 |
| Leave Service | 19 |
| Payroll Service | 23 |
| AI/ML Service | 8 |
| Reporting Service | 12 |
| Notification Service | 5 |
| Integration Hub | 7 |
| Dashboard | 1 |
| **Total** | **142** |

---

## 6. AI/ML Architecture

### 6.1 Service Overview

Python FastAPI service, stateless, no database. Loads serialized models at startup. All endpoints are inference-only — training happens offline.

### 6.2 Models

| Model | Algorithm | Purpose | Training Data | Key Metrics |
|-------|-----------|---------|---------------|-------------|
| Payroll Anomaly | Isolation Forest (scikit-learn) | Flag suspicious payslips | Historical payslips + injected anomalies | Precision ≥97%, Response <100ms |
| Attendance Fraud | Isolation Forest (scikit-learn) | Detect buddy-punching, spoofing | Attendance records + synthetic fraud | Precision ≥95%, Response <100ms |
| Attrition | XGBoost | Predict flight-risk employees | Terminated + active employees (features: tenure, salary, leave, attendance) | AUC ≥0.85, F1 ≥0.80 |
| Payroll Forecast | Prophet / ARIMA | Predict future payroll costs | 12+ months of historical payroll totals | MAPE <5% |

### 6.3 Payroll Anomaly — Features & Response

**Input features (14):**
```
earned_salary, net_salary, allowances, deductions,
work_ratio (worked/total days),
salary_zscore ((earned - historical_avg) / historical_std),
months_employed, allowance_ratio (allowances / earned),
deduction_ratio (deductions / earned),
ipn_deviation (actual IPN vs calculated IPN),
opv_deviation, is_new_employee (< 3 months),
previous_month_salary, salary_change_pct
```

**Response:**
```json
{
  "anomaly_score": 0.82,
  "is_anomaly": true,
  "confidence": 0.91,
  "flags": ["salary_spike", "unusual_allowance"],
  "recommendation": "REVIEW",
  "explanation": "Earned salary is 3.2σ above 6-month average. Allowances are 45% of earned (typical <10%)."
}
```

**Thresholds:** score < 0.3 → NORMAL, 0.3–0.65 → WARNING (log only), > 0.65 → REVIEW (set payslip to FLAGGED)

### 6.4 Attendance Fraud — Features & Response

**Input features (10):**
```
time_diff_minutes (since last check-in),
location_distance_meters, device_switch (bool),
hour_of_day, day_of_week,
historical_checkin_hour_avg, historical_checkin_hour_std,
checkins_today_count, days_since_last_checkin,
is_remote_worker (bool)
```

**Response:**
```json
{
  "fraud_probability": 0.87,
  "is_fraud": true,
  "flags": ["multiple_checkins", "suspicious_location"],
  "recommendation": "BLOCK"
}
```

**Actions:** score < 0.3 → allow, 0.3–0.65 → allow but flag, > 0.65 → block check-in + alert HR

### 6.5 Attrition Prediction — Features & Response

**Input features (14):**
```
tenure_months, salary_vs_position_avg,
salary_growth_rate_annual, months_since_last_promotion,
leave_usage_rate, sick_leave_frequency_quarterly,
late_attendance_rate, overtime_hours_monthly_avg,
team_turnover_last_6m, manager_change_count,
age, department_avg_tenure,
performance_rating (if available), engagement_proxy_score
```

**Response:**
```json
{
  "attrition_risk": 0.72,
  "risk_level": "HIGH",
  "top_factors": [
    {"factor": "salary_below_average", "impact": 0.35, "detail": "15% below position average"},
    {"factor": "no_promotion_18_months", "impact": 0.25},
    {"factor": "high_overtime", "impact": 0.20, "detail": "avg 25h/month (company avg: 8h)"}
  ],
  "recommended_actions": [
    "Schedule 1-on-1 retention conversation",
    "Review salary against position band",
    "Evaluate workload redistribution"
  ]
}
```

### 6.6 Integration Flow

```
1. Payroll Service generates payslip
   → POST /v1/ai/payroll/detect (Feign client)
   → if anomaly_score > 0.65:
       payslip.status = FLAGGED
       payslip.anomalyScore = score
       payslip.anomalyFlags = flags
       publish PayrollAnomalyDetectedEvent
   → else: payslip.status = DRAFT

2. Attendance Service receives check-in
   → check recent records for suspicious patterns
   → if suspicious: POST /v1/ai/attendance/fraud-detect
   → if BLOCK: reject check-in, publish FraudAttemptDetectedEvent
   → if WARNING: record with fraud_score, continue

3. HR Dashboard loads attrition widget
   → GET /v1/ai/attrition/risk?departmentId=...
   → display risk cards with recommended actions

4. Finance views forecast
   → GET /v1/ai/payroll/forecast?months=3
   → display predicted costs with confidence intervals
```

### 6.7 AI Service — Non-Critical Pattern

AI service failures must NEVER block business operations:

```java
try {
    AnomalyResponse result = aiMlClient.detectPayrollAnomaly(request);
    if (result.isAnomaly()) {
        payslip.setStatus(PayslipStatus.FLAGGED);
        payslip.setAnomalyScore(result.getAnomalyScore());
    }
} catch (Exception e) {
    log.warn("AI service unavailable — skipping anomaly detection: {}", e.getMessage());
    // Continue with DRAFT status — business is not blocked
}
```

---

## 7. 1C:Enterprise Integration

### 7.1 Data Flow

```
Payroll Approved → PayrollPeriodApprovedEvent (RabbitMQ)
    → Integration Hub subscribes
    → Fetches full payroll data from payroll-service (Feign)
    → Transforms to 1C JSON payload
    → POST to 1C HTTP Service: /hs/hrms/payroll/sync
    → On success: log 1C document ID
    → On failure: retry 3× with exponential backoff → dead-letter queue → alert HR
```

### 7.2 1C Payload Schema

```json
{
  "period": {"year": 2026, "month": 3, "startDate": "2026-03-01", "endDate": "2026-03-31"},
  "organization": {"bin": "123456789012", "name": "ТОО Компания", "kbe": "17"},
  "employees": [{
    "iin": "123456789012",
    "fullName": "Иванов Иван Иванович",
    "grossSalary": 300000.00,
    "earnedSalary": 300000.00,
    "opvAmount": 30000.00,
    "vosmsAmount": 6000.00,
    "ipnAmount": 13425.00,
    "netSalary": 250575.00,
    "soAmount": 13500.00,
    "snAmount": 18000.00,
    "opvrAmount": 10500.00,
    "workedDays": 22,
    "totalWorkingDays": 22,
    "isResident": true,
    "hasDisability": false
  }],
  "totals": {
    "totalGross": 15000000.00, "totalNet": 12528750.00,
    "totalOpv": 1500000.00, "totalVosms": 300000.00, "totalIpn": 671250.00,
    "totalSo": 675000.00, "totalSn": 900000.00, "totalOpvr": 525000.00,
    "employeeCount": 50
  }
}
```

---

## 8. Infrastructure

### 8.1 Docker Compose — Full Stack

```yaml
services:
  api-gateway:      { build: services/api-gateway, ports: ["8080:8080"] }
  user-service:     { build: services/user-service, ports: ["8081:8081"] }
  employee-service: { build: services/employee-service, ports: ["8082:8082"] }
  attendance-service: { build: services/attendance-service, ports: ["8083:8083"] }
  leave-service:    { build: services/leave-service, ports: ["8084:8084"] }
  payroll-service:  { build: services/payroll-service, ports: ["8085:8085"] }
  ai-ml-service:    { build: services/ai-ml-service, ports: ["8086:8086"] }
  reporting-service: { build: services/reporting-service, ports: ["8087:8087"] }
  notification-service: { build: services/notification-service, ports: ["8088:8088"] }
  integration-hub:  { build: services/integration-hub, ports: ["8089:8089"] }
  
  postgres:   { image: "postgres:16-alpine", ports: ["5432:5432"] }
  redis:      { image: "redis:7-alpine", ports: ["6379:6379"] }
  rabbitmq:   { image: "rabbitmq:3-management-alpine", ports: ["5672:5672","15672:15672"] }
  
  prometheus: { image: "prom/prometheus:latest", ports: ["9090:9090"] }
  grafana:    { image: "grafana/grafana:latest", ports: ["3001:3000"] }
```

### 8.2 CI/CD Pipeline

```
Push to develop → Test all services → Build Docker images → Push to GHCR → Deploy to staging
Push to main    → Test all services → Build Docker images → Push to GHCR → Deploy to production
                                                                            Health check → Rollback on failure
```

### 8.3 Monitoring

| Tool | Purpose |
|------|---------|
| Prometheus | Metrics collection (JVM, API latency, queue depth, cache hit rate) |
| Grafana | Dashboards (payroll processing time, error rates, AI model performance) |
| ELK Stack | Centralized logging across all services |
| Jaeger | Distributed tracing (request flow across services) |
| Spring Boot Actuator | Health checks, metrics endpoints per service |

---

## 9. Frontend Pages

| # | Page | Route | Key Components |
|---|------|-------|----------------|
| 1 | Login | /login | Email/password form, forgot-password link |
| 2 | Dashboard | /dashboard | Role-aware stat cards, charts, quick actions |
| 3 | Employee List | /employees | DataGrid, search, filters (dept/status/type), bulk actions |
| 4 | Employee Detail | /employees/:id | Tabs: Info, Salary History, Documents, Attendance, Leave, Timeline |
| 5 | Employee Form | /employees/new, /employees/:id/edit | Multi-step form with validation |
| 6 | Org Chart | /org-chart | Interactive tree visualization |
| 7 | Payroll Periods | /payroll | Period list, status badges, generate/approve/pay actions |
| 8 | Payslip Detail | /payroll/payslips/:id | Full breakdown with tax lines, anomaly flag indicator |
| 9 | My Payslips | /my-payslips | Employee self-service: monthly payslip list + PDF download |
| 10 | Leave Management | /leave | Tabs: My Requests, Balances, Pending Approval (managers) |
| 11 | Leave Calendar | /leave/calendar | Monthly calendar showing who's on leave per day |
| 12 | Attendance | /attendance | Check-in/out button, monthly calendar grid, summary stats |
| 13 | Reports | /reports | Cards per report type with download buttons |
| 14 | AI Insights | /ai-insights | Attrition risk list, anomaly overview, payroll forecast chart |
| 15 | User Management | /users | User list, role assignment, enable/disable |
| 16 | Settings | /settings | Company info, attendance config, leave config, notification config |
| 17 | My Profile | /profile | View/edit own info, change password |
| 18 | Notifications | (header dropdown) | Bell icon, unread badge, notification list, mark-all-read |

---

## 10. Kazakhstan Tax Code Reference

### 2026 Tax Code (Закон РК № 214-VIII от 18.07.2025)

```
МРП = 4 325 ₸        МЗП = 85 000 ₸

CALCULATION ORDER:
1. earnedSalary     = gross × (workedDays / totalDays)
2. OPV              = earned × 10%        cap 50×МЗП     skip if pensioner
3. ВОСМС            = earned × 2%         cap 20×МЗП
4. deduction        = 30×МРП (residents)  +882×МРП (disability grp3) +5000×МРП (grp1/2)
5. taxableIncome    = earned − OPV − ВОСМС − deduction   (floor 0)
6. IPN              = taxable × 10% (resident) or 20% (non-resident)
7. netSalary        = earned − OPV − ВОСМС − IPN + allowances − deductions

EMPLOYER (not deducted from employee):
8. SO               = (earned − OPV) × 5%
9. SN               = earned × 6%         (fixed, no longer −SO)
10. ОПВР            = earned × 3.5%
```

All monetary values: `BigDecimal` / `NUMERIC(15,2)`. Never `double`.
