-- Remove AI/fraud columns from attendance_records and drop any biometric audit tables
-- that may have been created on previously deployed databases.

ALTER TABLE hrms_attendance.attendance_records
    DROP COLUMN IF EXISTS fraud_score,
    DROP COLUMN IF EXISTS fraud_flags;

DROP TABLE IF EXISTS hrms_attendance.biometric_attempts;
DROP TABLE IF EXISTS hrms_attendance.biometric_data;