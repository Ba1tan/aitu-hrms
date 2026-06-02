-- V4 — work_schedules: persist working_days + description that the UI
-- has been collecting but the backend was silently dropping. working_days
-- is stored as a CSV of three-letter day codes (MON,TUE,WED,THU,FRI),
-- defaulting to the standard Mon-Fri so existing rows keep behaving the
-- way attendance-service has historically hardcoded.

ALTER TABLE work_schedules
    ADD COLUMN working_days VARCHAR(40) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI';

ALTER TABLE work_schedules
    ADD COLUMN description  TEXT;