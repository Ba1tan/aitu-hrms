-- V2 — Kazakhstan public holidays for 2026 (16 days).
-- Source: Labor Code of Kazakhstan, Article 84.

INSERT INTO holidays (name, holiday_date, is_annual, description) VALUES
    ('Новый год',                           '2026-01-01', TRUE,  'New Year''s Day'),
    ('Новый год',                           '2026-01-02', TRUE,  'New Year''s Day (2nd)'),
    ('Православное Рождество',              '2026-01-07', TRUE,  'Orthodox Christmas'),
    ('Международный женский день',          '2026-03-08', TRUE,  'International Women''s Day'),
    ('Наурыз мейрамы',                      '2026-03-21', TRUE,  'Nauryz'),
    ('Наурыз мейрамы',                      '2026-03-22', TRUE,  'Nauryz (2nd day)'),
    ('Наурыз мейрамы',                      '2026-03-23', TRUE,  'Nauryz (3rd day)'),
    ('Праздник единства народа Казахстана', '2026-05-01', TRUE,  'Kazakhstan People Unity Day'),
    ('День защитника Отечества',            '2026-05-07', TRUE,  'Defender of the Fatherland Day'),
    ('День Победы',                         '2026-05-09', TRUE,  'Victory Day'),
    ('День столицы',                        '2026-07-06', TRUE,  'Capital City Day'),
    ('Курбан айт',                          '2026-05-27', FALSE, 'Kurban Ait (Eid al-Adha) — moves yearly'),
    ('День Конституции',                    '2026-08-30', TRUE,  'Constitution Day'),
    ('День Республики',                     '2026-10-25', TRUE,  'Republic Day'),
    ('День Первого Президента',             '2026-12-01', TRUE,  'First President Day'),
    ('День Независимости',                  '2026-12-16', TRUE,  'Independence Day');