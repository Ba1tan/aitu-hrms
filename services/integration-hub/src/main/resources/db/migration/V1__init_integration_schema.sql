CREATE SCHEMA IF NOT EXISTS hrms_integration;

CREATE TABLE hrms_integration.sync_jobs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id        UUID NOT NULL,
    target           VARCHAR(20) NOT NULL CHECK (target IN ('ONE_C', 'BANK')),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','IN_PROGRESS','SUCCESS','FAILED','RETRYING')),
    payload          JSONB,
    response         JSONB,
    onec_document_id VARCHAR(100),
    error_message    TEXT,
    retry_count      INTEGER NOT NULL DEFAULT 0,
    max_retries      INTEGER NOT NULL DEFAULT 3,
    next_retry_at    TIMESTAMP,
    completed_at     TIMESTAMP,
    is_deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP,
    created_by       VARCHAR(255),
    updated_by       VARCHAR(255),
    version          INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_sync_period ON hrms_integration.sync_jobs(period_id);
CREATE INDEX idx_sync_status ON hrms_integration.sync_jobs(status);
CREATE INDEX idx_sync_retry  ON hrms_integration.sync_jobs(next_retry_at)
    WHERE status = 'RETRYING';

CREATE TABLE hrms_integration.company_settings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(100) NOT NULL UNIQUE,
    value       TEXT NOT NULL,
    description TEXT,
    category    VARCHAR(50) NOT NULL,
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(255)
);
CREATE INDEX idx_settings_key      ON hrms_integration.company_settings(key);
CREATE INDEX idx_settings_category ON hrms_integration.company_settings(category);

-- Local audit for settings changes (hrms_user.audit_logs has no write endpoint — see BACKLOG)
CREATE TABLE hrms_integration.settings_audit (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    user_email  VARCHAR(255),
    action      VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100),
    old_value   TEXT,
    new_value   TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_settings_audit_key  ON hrms_integration.settings_audit(setting_key);
CREATE INDEX idx_settings_audit_user ON hrms_integration.settings_audit(user_id);
