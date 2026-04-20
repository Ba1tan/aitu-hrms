-- V1 — hrms_employee schema: departments, positions, employees, salary_history,
-- employee_documents, emergency_contacts.
-- Schema `hrms_employee` is created upfront in scripts/init-db.sql.

-- Departments (self-referential; manager_id FK added after employees table exists).
CREATE TABLE departments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200)  NOT NULL,
    code            VARCHAR(50)   UNIQUE,
    description     TEXT,
    parent_id       UUID          REFERENCES departments(id) ON DELETE SET NULL,
    manager_id      UUID,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE INDEX idx_departments_parent ON departments(parent_id);

-- Positions
CREATE TABLE positions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200)  NOT NULL,
    department_id   UUID          REFERENCES departments(id) ON DELETE SET NULL,
    min_salary      NUMERIC(14,2),
    max_salary      NUMERIC(14,2),
    description     TEXT,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE INDEX idx_positions_department ON positions(department_id);

-- Employees (KZ payroll flags baked in: disability_group, is_resident, is_pensioner)
CREATE TABLE employees (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_number     VARCHAR(30)   NOT NULL UNIQUE,
    first_name          VARCHAR(100)  NOT NULL,
    last_name           VARCHAR(100)  NOT NULL,
    middle_name         VARCHAR(100),
    iin                 VARCHAR(12)   UNIQUE,
    email               VARCHAR(255),
    phone               VARCHAR(20),
    date_of_birth       DATE,
    gender              VARCHAR(10)   CHECK (gender IN ('MALE','FEMALE')),
    hire_date           DATE          NOT NULL,
    termination_date    DATE,
    termination_reason  TEXT,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
                           'ACTIVE','ON_LEAVE','PROBATION','TERMINATED','SUSPENDED')),
    employment_type     VARCHAR(20)   CHECK (employment_type IN (
                           'FULL_TIME','PART_TIME','CONTRACT','INTERN')),
    department_id       UUID          REFERENCES departments(id) ON DELETE SET NULL,
    position_id         UUID          REFERENCES positions(id)   ON DELETE SET NULL,
    manager_id          UUID          REFERENCES employees(id)   ON DELETE SET NULL,
    base_salary         NUMERIC(14,2) NOT NULL DEFAULT 0,
    disability_group    VARCHAR(10)   NOT NULL DEFAULT 'NONE' CHECK (disability_group IN (
                           'NONE','GROUP_1','GROUP_2','GROUP_3')),
    is_resident         BOOLEAN       NOT NULL DEFAULT TRUE,
    is_pensioner        BOOLEAN       NOT NULL DEFAULT FALSE,
    address             TEXT,
    is_deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);
CREATE INDEX idx_employees_status     ON employees(status)        WHERE is_deleted = FALSE;
CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_manager    ON employees(manager_id);
CREATE INDEX idx_employees_iin        ON employees(iin) WHERE iin IS NOT NULL;

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager
    FOREIGN KEY (manager_id) REFERENCES employees(id) ON DELETE SET NULL;

-- Salary history (every change audited with approver + reason)
CREATE TABLE salary_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    previous_salary NUMERIC(14,2),
    new_salary      NUMERIC(14,2) NOT NULL,
    effective_date  DATE          NOT NULL,
    reason          TEXT,
    approved_by     UUID,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE INDEX idx_salary_history_employee ON salary_history(employee_id, effective_date DESC);

-- Employee documents (files live on disk under app.storage.base-path)
CREATE TABLE employee_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    document_type   VARCHAR(50)   NOT NULL,
    file_name       VARCHAR(255)  NOT NULL,
    storage_path    VARCHAR(500)  NOT NULL,
    content_type    VARCHAR(100),
    file_size       BIGINT,
    expiry_date     DATE,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE INDEX idx_documents_employee ON employee_documents(employee_id) WHERE is_deleted = FALSE;

-- Emergency contacts
CREATE TABLE emergency_contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    name            VARCHAR(200)  NOT NULL,
    relationship    VARCHAR(50),
    phone           VARCHAR(20)   NOT NULL,
    email           VARCHAR(255),
    is_primary      BOOLEAN       NOT NULL DEFAULT FALSE,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);
CREATE INDEX idx_emergency_contacts_employee ON emergency_contacts(employee_id) WHERE is_deleted = FALSE;