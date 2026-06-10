-- V3 — Store the employee's bank account (KZ IBAN) + bank name.
-- The frontend already collects these on the create/edit form, but the backend
-- was silently dropping them; payroll bank-file generation needs the IBAN.

ALTER TABLE hrms_employee.employees
    ADD COLUMN IF NOT EXISTS bank_account VARCHAR(34),
    ADD COLUMN IF NOT EXISTS bank_name    VARCHAR(100);
