-- V1 — hrms_user schema: users, permissions, role_permissions, audit_logs
-- Schema `hrms_user` is created upfront in scripts/init-db.sql.

CREATE TABLE users (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name              VARCHAR(100)  NOT NULL,
    last_name               VARCHAR(100)  NOT NULL,
    email                   VARCHAR(255)  NOT NULL UNIQUE,
    password                VARCHAR(255)  NOT NULL,
    role                    VARCHAR(20)   NOT NULL CHECK (role IN (
                               'SUPER_ADMIN','DIRECTOR','HR_MANAGER','HR_SPECIALIST',
                               'ACCOUNTANT','MANAGER','TEAM_LEAD','EMPLOYEE')),
    phone                   VARCHAR(20),
    enabled                 BOOLEAN       NOT NULL DEFAULT TRUE,
    account_non_locked      BOOLEAN       NOT NULL DEFAULT TRUE,
    employee_id             UUID,
    require_password_change BOOLEAN       NOT NULL DEFAULT FALSE,
    last_login_at           TIMESTAMP,
    last_login_ip           VARCHAR(45),
    failed_login_count      INTEGER       NOT NULL DEFAULT 0,
    locked_until            TIMESTAMP,
    is_deleted              BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);
CREATE INDEX idx_users_role       ON users(role)        WHERE is_deleted = FALSE;
CREATE INDEX idx_users_employee   ON users(employee_id) WHERE employee_id IS NOT NULL;

CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    module      VARCHAR(50)  NOT NULL
);

CREATE TABLE role_permissions (
    role          VARCHAR(20) NOT NULL,
    permission_id UUID        NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role, permission_id)
);

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
CREATE INDEX idx_audit_user   ON audit_logs(user_id);
CREATE INDEX idx_audit_date   ON audit_logs(created_at);