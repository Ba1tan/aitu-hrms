INSERT INTO hrms_integration.company_settings (key, value, description, category)
VALUES
    ('company.timezone',            'Asia/Almaty',       'IANA timezone name',                   'company'),
    ('company.currency',            'KZT',               'ISO 4217 currency code',               'company'),
    ('company.locale_default',      'ru',                'Default locale: ru | kk | en',         'company'),
    ('company.tax_resident',        'true',              'Affects IPN rate for company entity',  'company'),
    ('attendance.check_in_methods', '["WEB","FACE"]',    'Allowed check-in methods JSON',        'attendance'),
    ('attendance.require_face',     'false',             'Force biometric check-in',             'attendance'),
    ('payroll.payslip_release_day', '5',                 'Day of month payslips become visible', 'payroll'),
    ('leave.annual_carryover_max_pct', '50',             'Max carryover percent',                'leave'),
    ('integration.bank_default_format', 'KASPI_TSV',     'Default bank file format',             'integration'),
    ('setup.completed',             'false',             'Setup wizard completion flag',         'system')
ON CONFLICT (key) DO NOTHING;
