CREATE SCHEMA IF NOT EXISTS hrms_notification;

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
CREATE INDEX idx_notif_unread ON hrms_notification.notifications(user_id, is_read)
    WHERE is_read = FALSE;
