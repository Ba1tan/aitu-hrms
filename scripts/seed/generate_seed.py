#!/usr/bin/env python3
"""Generate the demo-company seed migrations.

Emits two Flyway migrations with consistent fixed UUIDs so the user accounts in
hrms_user link to the employees in hrms_employee (no cross-schema FK exists):

  services/employee-service/.../V2__seed_demo_company.sql
  services/user-service/.../V4__seed_demo_users.sql

Re-run after editing the data below to regenerate both files.
"""
import os

# All seeded login accounts share this BCrypt(cost 12) hash.
PASSWORD_HASH = "$2a$12$Mop8tWI8HeEnTTX7/sxDd.0Jdz9f0zcd9VtB4wxxa8S9TsU7z3uAa"

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))

def uid(prefix_digit, n):
    """Deterministic UUID, e.g. 33333333-3333-3333-3333-000000000007."""
    d = str(prefix_digit)
    return f"{d*8}-{d*4}-{d*4}-{d*4}-{n:012d}"

DEPT, POS, EMP, USR = 1, 2, 3, 4  # uuid prefix digits

# ── Departments: (id, name, code, parent_id) ────────────────────────────────
departments = [
    (1, "Управление",            "EXEC",  None),
    (2, "Отдел кадров",          "HR",    1),
    (3, "Финансовый отдел",      "FIN",   1),
    (4, "Разработка",            "ENG",   1),
    (5, "Продажи",               "SALES", 1),
    (6, "Маркетинг",             "MKT",   1),
    (7, "Операционный отдел",    "OPS",   1),
    (8, "Поддержка клиентов",    "SUP",   7),
]
# department head employee id (set via UPDATE after employees exist)
dept_head = {1: 1, 2: 2, 3: 5, 4: 8, 5: 24, 6: 33, 7: 38, 8: 43}

# ── Positions: (id, title, dept, min, max) ──────────────────────────────────
positions = [
    (1,  "Генеральный директор",      1, 1200000, 1500000),
    (2,  "HR-менеджер",               2,  500000,  700000),
    (3,  "HR-специалист",             2,  300000,  450000),
    (4,  "Финансовый директор",       3,  900000, 1200000),
    (5,  "Бухгалтер",                 3,  400000,  600000),
    (6,  "Руководитель разработки",   4,  900000, 1200000),
    (7,  "Senior-разработчик",        4,  700000, 1000000),
    (8,  "Middle-разработчик",        4,  450000,  700000),
    (9,  "Junior-разработчик",        4,  250000,  450000),
    (10, "QA-инженер",                4,  350000,  550000),
    (11, "DevOps-инженер",            4,  600000,  900000),
    (12, "Руководитель продаж",       5,  700000, 1000000),
    (13, "Менеджер по продажам",      5,  300000,  600000),
    (14, "Руководитель маркетинга",   6,  700000,  950000),
    (15, "Маркетолог",                6,  350000,  550000),
    (16, "Операционный директор",     7,  800000, 1100000),
    (17, "Специалист по логистике",   7,  350000,  500000),
    (18, "Руководитель поддержки",    8,  450000,  650000),
    (19, "Специалист поддержки",      8,  250000,  400000),
    (20, "Системный администратор",   7,  400000,  600000),
]

# ── Employees ───────────────────────────────────────────────────────────────
# (ru_first, ru_last, lat_first, lat_last, gender)
NAMES = [
    ("Нурсултан","Абдразаков","nursultan","abdrazakov","MALE"),
    ("Айгерим","Касымова","aigerim","kassymova","FEMALE"),
    ("Данияр","Сейтказин","daniyar","seitkazin","MALE"),
    ("Аружан","Жумабекова","aruzhan","zhumabekova","FEMALE"),
    ("Ержан","Тулегенов","yerzhan","tulegenov","MALE"),
    ("Сабина","Оспанова","sabina","ospanova","FEMALE"),
    ("Тимур","Ахметов","timur","akhmetov","MALE"),
    ("Алибек","Нурланов","alibek","nurlanov","MALE"),
    ("Мадина","Сулейменова","madina","suleimenova","FEMALE"),
    ("Арман","Бекжанов","arman","bekzhanov","MALE"),
    ("Дамир","Исмаилов","damir","ismailov","MALE"),
    ("Камила","Ерланова","kamila","yerlanova","FEMALE"),
    ("Руслан","Жаксыбеков","ruslan","zhaksybekov","MALE"),
    ("Аян","Серикбаев","ayan","serikbayev","MALE"),
    ("Дина","Маратова","dina","maratova","FEMALE"),
    ("Бекзат","Алимов","bekzat","alimov","MALE"),
    ("Жанель","Кенжебекова","zhanel","kenzhebekova","FEMALE"),
    ("Олжас","Дюсенов","olzhas","dyusenov","MALE"),
    ("Айбек","Турсынов","aibek","tursynov","MALE"),
    ("Сания","Габдуллина","saniya","gabdullina","FEMALE"),
    ("Нурлан","Койшыбаев","nurlan","koishybayev","MALE"),
    ("Алия","Рахимова","aliya","rakhimova","FEMALE"),
    ("Канат","Бейсенов","kanat","beisenov","MALE"),
    ("Жанна","Аскарова","zhanna","askarova","FEMALE"),
    ("Самат","Орынбаев","samat","orynbayev","MALE"),
    ("Динара","Шаймерденова","dinara","shaimerdenova","FEMALE"),
    ("Азамат","Кудайбергенов","azamat","kudaibergenov","MALE"),
    ("Лаура","Бектурганова","laura","bekturganova","FEMALE"),
    ("Ескендир","Молдабеков","yeskendir","moldabekov","MALE"),
    ("Гульнара","Сатпаева","gulnara","satpayeva","FEMALE"),
    ("Бауыржан","Темиров","bauyrzhan","temirov","MALE"),
    ("Асель","Нургалиева","assel","nurgaliyeva","FEMALE"),
    ("Марат","Жунусов","marat","zhunussov","MALE"),
    ("Индира","Сапарова","indira","saparova","FEMALE"),
    ("Талгат","Ибраев","talgat","ibrayev","MALE"),
    ("Карина","Досжанова","karina","doszhanova","FEMALE"),
    ("Ильяс","Мухамеджанов","ilyas","mukhamedzhanov","MALE"),
    ("Жомарт","Калиев","zhomart","kaliyev","MALE"),
    ("Айдана","Естаева","aidana","yestayeva","FEMALE"),
    ("Санжар","Омаров","sanzhar","omarov","MALE"),
    ("Меруерт","Жанатова","meruyert","zhanatova","FEMALE"),
    ("Дастан","Кубенов","dastan","kubenov","MALE"),
    ("Алтынай","Болатова","altynai","bolatova","FEMALE"),
    ("Рустем","Адилов","rustem","adilov","MALE"),
    ("Венера","Ташенова","venera","tashenova","FEMALE"),
    ("Кайрат","Смагулов","kairat","smagulov","MALE"),
    ("Назым","Айтжанова","nazym","aitzhanova","FEMALE"),
    ("Ерлан","Каримов","yerlan","karimov","MALE"),
    ("Сауле","Жолдасова","saule","zholdassova","FEMALE"),
    ("Багдат","Сериков","bagdat","serikov","MALE"),
]

# (emp_id, position_id, dept_id, manager_id|None, role, employment_type)
EMPLOYEE = "EMPLOYEE"
emp_rows = [
    (1,  1, 1, None, "DIRECTOR",      "FULL_TIME"),
    (2,  2, 2, 1,    "HR_MANAGER",    "FULL_TIME"),
    (3,  3, 2, 2,    "HR_SPECIALIST", "FULL_TIME"),
    (4,  3, 2, 2,    "HR_SPECIALIST", "FULL_TIME"),
    (5,  4, 3, 1,    "ACCOUNTANT",    "FULL_TIME"),
    (6,  5, 3, 5,    "ACCOUNTANT",    "FULL_TIME"),
    (7,  5, 3, 5,    "ACCOUNTANT",    "FULL_TIME"),
    (8,  6, 4, 1,    "MANAGER",       "FULL_TIME"),
    (9,  7, 4, 8,    "TEAM_LEAD",     "FULL_TIME"),
    (10, 7, 4, 8,    "TEAM_LEAD",     "FULL_TIME"),
    (11, 8, 4, 9,    EMPLOYEE,        "FULL_TIME"),
    (12, 8, 4, 9,    EMPLOYEE,        "FULL_TIME"),
    (13, 8, 4, 9,    EMPLOYEE,        "FULL_TIME"),
    (14, 8, 4, 10,   EMPLOYEE,        "FULL_TIME"),
    (15, 8, 4, 10,   EMPLOYEE,        "FULL_TIME"),
    (16, 8, 4, 10,   EMPLOYEE,        "FULL_TIME"),
    (17, 9, 4, 9,    EMPLOYEE,        "FULL_TIME"),
    (18, 9, 4, 10,   EMPLOYEE,        "FULL_TIME"),
    (19, 9, 4, 9,    EMPLOYEE,        "INTERN"),
    (20, 9, 4, 10,   EMPLOYEE,        "INTERN"),
    (21, 10, 4, 8,   EMPLOYEE,        "FULL_TIME"),
    (22, 10, 4, 8,   EMPLOYEE,        "FULL_TIME"),
    (23, 11, 4, 8,   EMPLOYEE,        "FULL_TIME"),
    (24, 12, 5, 1,   "MANAGER",       "FULL_TIME"),
    (25, 13, 5, 24,  EMPLOYEE,        "FULL_TIME"),
    (26, 13, 5, 24,  EMPLOYEE,        "FULL_TIME"),
    (27, 13, 5, 24,  EMPLOYEE,        "FULL_TIME"),
    (28, 13, 5, 24,  EMPLOYEE,        "FULL_TIME"),
    (29, 13, 5, 24,  EMPLOYEE,        "FULL_TIME"),
    (30, 13, 5, 24,  EMPLOYEE,        "PART_TIME"),
    (31, 13, 5, 24,  EMPLOYEE,        "FULL_TIME"),
    (32, 13, 5, 24,  EMPLOYEE,        "FULL_TIME"),
    (33, 14, 6, 1,   "MANAGER",       "FULL_TIME"),
    (34, 15, 6, 33,  EMPLOYEE,        "FULL_TIME"),
    (35, 15, 6, 33,  EMPLOYEE,        "FULL_TIME"),
    (36, 15, 6, 33,  EMPLOYEE,        "FULL_TIME"),
    (37, 15, 6, 33,  EMPLOYEE,        "PART_TIME"),
    (38, 16, 7, 1,   "MANAGER",       "FULL_TIME"),
    (39, 17, 7, 38,  EMPLOYEE,        "FULL_TIME"),
    (40, 17, 7, 38,  EMPLOYEE,        "FULL_TIME"),
    (41, 17, 7, 38,  EMPLOYEE,        "FULL_TIME"),
    (42, 20, 7, 38,  EMPLOYEE,        "FULL_TIME"),
    (43, 18, 8, 38,  "TEAM_LEAD",     "FULL_TIME"),
    (44, 19, 8, 43,  EMPLOYEE,        "FULL_TIME"),
    (45, 19, 8, 43,  EMPLOYEE,        "FULL_TIME"),
    (46, 19, 8, 43,  EMPLOYEE,        "FULL_TIME"),
    (47, 19, 8, 43,  EMPLOYEE,        "FULL_TIME"),
    (48, 19, 8, 43,  EMPLOYEE,        "FULL_TIME"),
    (49, 19, 8, 43,  EMPLOYEE,        "PART_TIME"),
    (50, 19, 8, 43,  EMPLOYEE,        "FULL_TIME"),
]

pos_band = {p[0]: (p[3], p[4]) for p in positions}

def salary_for(pos_id, n):
    lo, hi = pos_band[pos_id]
    step = (hi - lo) // 5 or 1
    return lo + (n % 6) * step  # deterministic within band, multiples of 10k-ish

def iin_for(n, gender):
    # YYMMDD + century/gender digit + 4-digit serial + 1 check digit (12 total).
    yy = (n * 7 + 80) % 100
    mm = (n % 12) + 1
    dd = (n % 27) + 1
    cg = "3" if gender == "MALE" else "4"
    return f"{yy:02d}{mm:02d}{dd:02d}{cg}{n:04d}{(n*3) % 10}"

def dob_for(n):
    year = 1975 + (n * 3) % 28      # 1975..2002
    mm = (n % 12) + 1
    dd = (n % 27) + 1
    return f"{year:04d}-{mm:02d}-{dd:02d}"

def hire_for(n):
    year = 2021 + (n % 5)           # 2021..2025
    mm = (n % 12) + 1
    dd = (n % 27) + 1
    return f"{year:04d}-{mm:02d}-{dd:02d}"

def sql_str(s):
    return "'" + s.replace("'", "''") + "'"

def emp(n):
    return NAMES[n - 1]

# ── Emit employee-service seed ──────────────────────────────────────────────
def gen_employee_sql():
    L = []
    L.append("-- V2 — DEMO company seed: 8 departments, 20 positions, 50 employees.")
    L.append("-- Generated by scripts/seed/generate_seed.py. DEMO DATA — safe to drop.")
    L.append("-- UUIDs are fixed so hrms_user accounts can link via users.employee_id.")
    L.append("")
    L.append("INSERT INTO departments (id, name, code, parent_id, created_by) VALUES")
    rows = []
    for did, name, code, parent in departments:
        p = f"{sql_str(uid(DEPT, parent))}" if parent else "NULL"
        rows.append(f"  ({sql_str(uid(DEPT, did))}, {sql_str(name)}, {sql_str(code)}, {p}, 'seed')")
    L.append(",\n".join(rows) + ";")
    L.append("")
    L.append("INSERT INTO positions (id, title, department_id, min_salary, max_salary, created_by) VALUES")
    rows = []
    for pid, title, dep, lo, hi in positions:
        rows.append(f"  ({sql_str(uid(POS, pid))}, {sql_str(title)}, {sql_str(uid(DEPT, dep))}, {lo}, {hi}, 'seed')")
    L.append(",\n".join(rows) + ";")
    L.append("")
    L.append("INSERT INTO employees (id, employee_number, first_name, last_name, iin, email, phone,")
    L.append("    date_of_birth, gender, hire_date, status, employment_type,")
    L.append("    department_id, position_id, manager_id, base_salary, is_resident, disability_group, created_by) VALUES")
    rows = []
    for (eid, pid, dep, mgr, role, etype) in emp_rows:
        rf, rl, lf, ll, gender = emp(eid)
        email = f"{lf}.{ll}@hrms.kz"
        phone = f"+77{(700000000 + eid*1234567) % 1000000000:09d}"
        mgr_sql = sql_str(uid(EMP, mgr)) if mgr else "NULL"
        disab = "'GROUP_3'" if eid == 50 else "'NONE'"
        rows.append(
            f"  ({sql_str(uid(EMP, eid))}, 'EMP-2026-{eid:03d}', {sql_str(rf)}, {sql_str(rl)}, "
            f"'{iin_for(eid, gender)}', {sql_str(email)}, '{phone}', '{dob_for(eid)}', '{gender}', "
            f"'{hire_for(eid)}', 'ACTIVE', '{etype}', {sql_str(uid(DEPT, dep))}, {sql_str(uid(POS, pid))}, "
            f"{mgr_sql}, {salary_for(pid, eid)}, TRUE, {disab}, 'seed')")
    L.append(",\n".join(rows) + ";")
    L.append("")
    L.append("-- Department heads (circular FK resolved after employees exist).")
    for did, head in dept_head.items():
        L.append(f"UPDATE departments SET manager_id = {sql_str(uid(EMP, head))} WHERE id = {sql_str(uid(DEPT, did))};")
    L.append("")
    return "\n".join(L)

# ── Emit user-service seed ──────────────────────────────────────────────────
def gen_user_sql():
    L = []
    L.append("-- V4 — DEMO login accounts for the 50 seeded employees.")
    L.append("-- Generated by scripts/seed/generate_seed.py. DEMO DATA — safe to drop.")
    L.append("-- All accounts share one BCrypt password; employee_id links to hrms_employee.")
    L.append("")
    L.append("INSERT INTO users (id, first_name, last_name, email, password, role, phone,")
    L.append("    enabled, account_non_locked, employee_id, require_password_change, created_by) VALUES")
    rows = []
    for (eid, pid, dep, mgr, role, etype) in emp_rows:
        rf, rl, lf, ll, gender = emp(eid)
        email = f"{lf}.{ll}@hrms.kz"
        phone = f"+77{(700000000 + eid*1234567) % 1000000000:09d}"
        rows.append(
            f"  ({sql_str(uid(USR, eid))}, {sql_str(rf)}, {sql_str(rl)}, {sql_str(email)}, "
            f"{sql_str(PASSWORD_HASH)}, '{role}', '{phone}', TRUE, TRUE, "
            f"{sql_str(uid(EMP, eid))}, FALSE, 'seed')")
    L.append(",\n".join(rows) + ";")
    L.append("")
    return "\n".join(L)

def main():
    emp_path = os.path.join(ROOT, "services/employee-service/src/main/resources/db/migration/V2__seed_demo_company.sql")
    usr_path = os.path.join(ROOT, "services/user-service/src/main/resources/db/migration/V4__seed_demo_users.sql")
    with open(emp_path, "w") as f:
        f.write(gen_employee_sql())
    with open(usr_path, "w") as f:
        f.write(gen_user_sql())
    print("wrote", emp_path)
    print("wrote", usr_path)

if __name__ == "__main__":
    main()
