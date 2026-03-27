-- scripts/init-db.sql
-- Runs automatically when postgres container first starts (docker-entrypoint-initdb.d)
-- Creates extensions and placeholder schemas for future microservice extraction.
-- For MVP (monolith): everything runs in the 'public' schema of the 'hrms' database.

-- Enable UUID generation (required for gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS hrms_user;
CREATE SCHEMA IF NOT EXISTS hrms_employee;
CREATE SCHEMA IF NOT EXISTS hrms_attendance;
CREATE SCHEMA IF NOT EXISTS hrms_leave;
CREATE SCHEMA IF NOT EXISTS hrms_payroll;
CREATE SCHEMA IF NOT EXISTS hrms_notification;
CREATE SCHEMA IF NOT EXISTS hrms_reporting;
CREATE SCHEMA IF NOT EXISTS hrms_integration;

GRANT ALL PRIVILEGES ON SCHEMA public TO hrms_user;
GRANT ALL PRIVILEGES ON SCHEMA hrms_user TO hrms_user;
GRANT ALL PRIVILEGES ON SCHEMA hrms_employee TO hrms_user;
GRANT ALL PRIVILEGES ON SCHEMA hrms_attendance TO hrms_user;
GRANT ALL PRIVILEGES ON SCHEMA hrms_leave TO hrms_user;
GRANT ALL PRIVILEGES ON SCHEMA hrms_payroll TO hrms_user;
GRANT ALL PRIVILEGES ON SCHEMA hrms_notification TO hrms_user;
GRANT ALL PRIVILEGES ON SCHEMA hrms_reporting TO hrms_user;
GRANT ALL PRIVILEGES ON SCHEMA hrms_integration TO hrms_user;
