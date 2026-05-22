INSERT INTO hrms_integration.company_settings (key, value, description, category)
VALUES
    -- Required company keys (blank values are filled in via the /setup wizard;
    -- a blank row is still treated as "missing" by setup-status, but it must
    -- exist because PUT /v1/settings/{key} only updates existing rows).
    ('company.name',                    '',                  'Legal company name',                   'company'),
    ('company.bin',                     '',                  'KZ Business Identification Number (12 digits)', 'company'),
    ('company.legal_address',           '',                  'Legal address for tax forms',          'company'),
    ('company.timezone',                'Asia/Almaty',       'IANA timezone name',                   'company'),
    ('company.currency',                'KZT',               'ISO 4217 currency code',               'company'),
    ('company.locale_default',          'ru',                'Default locale: ru | kk | en',         'company'),
    ('company.tax_resident',            'true',              'Affects IPN rate for company entity',  'company'),
    -- Required attendance keys
    ('attendance.check_in_methods',     '["WEB"]',           'Allowed check-in methods JSON',        'attendance'),
    ('attendance.require_face',         'false',             'Force biometric check-in',             'attendance'),
    ('attendance.work_schedule_default_id', '',              'FK into hrms_attendance.work_schedules', 'attendance'),
    -- Optional payroll / leave keys
    ('payroll.payslip_release_day',     '5',                 'Day of month payslips become visible', 'payroll'),
    ('leave.annual_carryover_max_pct',  '50',                'Max carryover percent',                'leave'),
    -- Optional integration keys (editable later; rows must exist to be settable)
    ('integration.1c_base_url',         '',                  '1C OData base URL; empty disables sync', 'integration'),
    ('integration.1c_username',         '',                  '1C basic-auth username',               'integration'),
    ('integration.1c_password',         '',                  '1C basic-auth password (encrypted at rest)', 'integration'),
    ('integration.bank_default_format', 'KASPI_TSV',         'Default bank file format',             'integration'),
    ('setup.completed',                 'false',             'Setup wizard completion flag',         'system')
ON CONFLICT (key) DO NOTHING;
