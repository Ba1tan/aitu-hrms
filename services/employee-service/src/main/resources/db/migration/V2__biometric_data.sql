-- V2 — biometric_data: face/fingerprint enrollment per employee.
--
-- Photos themselves live on disk under app.storage.base-path
-- (/data/hrms/uploads/employees/{id}/biometric/face_N.jpg); this table
-- stores relative URLs and a pointer to the AI-service embedding file.

CREATE TABLE biometric_data (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID          NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    method          VARCHAR(20)   NOT NULL CHECK (method IN ('FACE','FINGERPRINT')),
    embedding_path  VARCHAR(500),
    photo_urls      TEXT[]        NOT NULL DEFAULT ARRAY[]::TEXT[],
    enrolled_at     TIMESTAMP,
    enrolled_by     UUID,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

-- One active enrollment per employee (partial unique index — deleted rows
-- are ignored so we can re-enroll after termination / re-hire).
CREATE UNIQUE INDEX idx_biometric_employee_unique
    ON biometric_data(employee_id) WHERE is_deleted = FALSE;