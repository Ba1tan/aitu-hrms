-- Drop biometric_data on previously deployed databases. V2 used to create this
-- table; the file has been deleted, but databases already past V2 still hold it.

DROP TABLE IF EXISTS hrms_employee.biometric_data;