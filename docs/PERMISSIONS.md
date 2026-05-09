# PERMISSIONS — Canonical Catalog

**Status:** authoritative source of truth for permission codes and role mappings.
If any other doc disagrees with this file, **this file wins** — open a PR to
update the other doc, do not invent new codes.

This catalog covers:
1. The 8 system roles
2. Every permission code referenced by `@PreAuthorize(...)` in service code today
3. Permission codes reserved for the four pending services (ai-ml, reporting, notification, integration-hub) so Askar can adopt them without re-litigating names

Permissions are flat strings. Spring Security `hasAuthority('CODE')` matches
exactly — no `ROLE_` prefix, no wildcards. Roles are mapped to permissions in
`hrms_user.role_permissions`; roles also get a `ROLE_` prefix from
`AuthService` so `hasRole(...)` and `hasAuthority(...)` both work.

---

## 1. Roles

| Code | Population | Description |
|------|------------|-------------|
| `SUPER_ADMIN` | 1–2% | Full system access. User/role management, settings, audit log, integration config. |
| `DIRECTOR` | 2–3% | Read-only executive view. Company-wide salary/headcount/dashboards. No mutations. |
| `HR_MANAGER` | 5–8% | Employee lifecycle, payroll processing & approval, leave management, all reports. |
| `HR_SPECIALIST` | 5–10% | Employee CRUD (no termination), document upload, leave processing. |
| `ACCOUNTANT` | 1–2% | Payroll calculation/payment, payslip review, tax exports, 1C sync. |
| `MANAGER` | 10–15% | Department head. Approves leave for direct reports, sees team attendance, no salary. |
| `TEAM_LEAD` | 5–10% | Sub-team lead. Approves leave for direct reports only. |
| `EMPLOYEE` | 60–70% | Default. Own profile, own attendance, own leave, own payslip. |

> **One employee can have only one role.** Multi-role workflows are deliberately
> out of scope — split duties across separate user accounts if needed.

---

## 2. Permission codes

Each row lists: code, owner service (where the @PreAuthorize lives or will live),
status, and which roles get it by default. Statuses:
- ✅ **Implemented** — at least one `@PreAuthorize('CODE')` exists in the codebase today
- 🟡 **Reserved** — referenced in API contract or planning doc; no @PreAuthorize yet (pending services)

### 2.1 Employee domain (employee-service)

| Code | Status | Default roles | Used by |
|------|--------|---------------|---------|
| `EMPLOYEE_CREATE` | ✅ | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST | `POST /v1/employees` |
| `EMPLOYEE_READ` | ✅ | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST, MANAGER, TEAM_LEAD, ACCOUNTANT, DIRECTOR | `GET /v1/employees/export`, generic listing where `_ALL`/`_OWN` doesn't fit |
| `EMPLOYEE_UPDATE` | ✅ | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST | `PUT /v1/employees/{id}`, `PATCH .../status` |
| `EMPLOYEE_DELETE` | ✅ | SUPER_ADMIN, HR_MANAGER | `DELETE /v1/employees/{id}`, `POST .../terminate` |
| `EMPLOYEE_VIEW_ALL` | ✅ | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST, ACCOUNTANT, DIRECTOR | List / detail of any employee |
| `EMPLOYEE_VIEW_TEAM` | ✅ | MANAGER, TEAM_LEAD | List / detail for direct reports only (filter applied in service) |
| `EMPLOYEE_VIEW_OWN` | ✅ | EMPLOYEE (and all higher roles implicitly) | Own profile, own biometric status |
| `EMPLOYEE_DOCUMENTS` | ✅ | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST | Upload / delete employee documents |
| `EMPLOYEE_BIOMETRIC` | ✅ | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST | Enroll / delete face data |
| `EMPLOYEE_SALARY_CHANGE` | 🟡 | SUPER_ADMIN, HR_MANAGER | `POST /v1/employees/{id}/salary-change` (currently gated by `EMPLOYEE_UPDATE` — split out before payroll audit) |
| `EMPLOYEE_SALARY_VIEW` | 🟡 | SUPER_ADMIN, HR_MANAGER, ACCOUNTANT, DIRECTOR | `GET /v1/employees/{id}/salary-history` |
| `DEPT_MANAGE` | ✅ | SUPER_ADMIN, HR_MANAGER | Departments + positions CRUD |

### 2.2 Attendance domain (attendance-service)

| Code | Status | Default roles | Used by |
|------|--------|---------------|---------|
| `ATTENDANCE_CHECKIN` | ✅ | EMPLOYEE (all roles) | `POST /v1/attendance/check-in[/face]`, `POST .../check-out[/face]` |
| `ATTENDANCE_VIEW_ALL` | ✅ | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST, DIRECTOR | Company-wide records & summaries |
| `ATTENDANCE_VIEW_TEAM` | ✅ | MANAGER, TEAM_LEAD | Department/team records (filter applied in service) |
| `ATTENDANCE_MANAGE` | ✅ | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST | Manual entry, holidays, work schedules |

### 2.3 Leave domain (leave-service)

| Code | Status | Default roles | Used by |
|------|--------|---------------|---------|
| `LEAVE_REQUEST_OWN` | ✅ | EMPLOYEE (all roles) | Submit / cancel own leave |
| `LEAVE_APPROVE_TEAM` | ✅ | MANAGER, TEAM_LEAD | Approve leave for direct reports |
| `LEAVE_APPROVE_ALL` | ✅ | SUPER_ADMIN, HR_MANAGER | Approve any leave request |
| `LEAVE_BALANCE_MANAGE` | ✅ | SUPER_ADMIN, HR_MANAGER | Adjust balances, run carryover, manage leave types |

### 2.4 Payroll domain (payroll-service)

| Code | Status | Default roles | Used by |
|------|--------|---------------|---------|
| `PAYROLL_VIEW` | ✅ | SUPER_ADMIN, HR_MANAGER, ACCOUNTANT, DIRECTOR | List payroll periods, period detail |
| `PAYROLL_READ_ALL` | ✅ | SUPER_ADMIN, HR_MANAGER, ACCOUNTANT | Read any payslip / addition / period totals |
| `PAYROLL_PROCESS` | ✅ | SUPER_ADMIN, HR_MANAGER, ACCOUNTANT | Create period, run calculation |
| `PAYROLL_ADJUST` | ✅ | SUPER_ADMIN, HR_MANAGER, ACCOUNTANT | Add/edit additions and deductions |
| `PAYROLL_APPROVE` | ✅ | SUPER_ADMIN, HR_MANAGER | Approve a calculated period (locks payslips) |
| `PAYROLL_PAY` | ✅ | SUPER_ADMIN, ACCOUNTANT | Mark period paid, generate bank file |
| `PAYSLIP_VIEW_OWN` | ✅ | EMPLOYEE (all roles) | Own payslips only |
| `PAYSLIP_ADJUST` | ✅ | SUPER_ADMIN, HR_MANAGER, ACCOUNTANT | Per-employee correction within an approved period |

### 2.5 Reporting domain (reporting-service — pending)

| Code | Status | Default roles | Used by |
|------|--------|---------------|---------|
| `REPORT_PAYROLL` | 🟡 | SUPER_ADMIN, HR_MANAGER, ACCOUNTANT | Payroll XLSX/PDF, Form 200.00, salary breakdown |
| `REPORT_ATTENDANCE` | 🟡 | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST | Attendance monthly / summary |
| `REPORT_LEAVE` | 🟡 | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST | Leave balances export |
| `REPORT_EXECUTIVE` | 🟡 | SUPER_ADMIN, DIRECTOR | Executive PDF summary |
| `REPORT_HR` | 🟡 | SUPER_ADMIN, HR_MANAGER, HR_SPECIALIST | Employee directory, headcount, turnover |

> Dashboard (`/v1/dashboard/stats`) is **not** permission-gated; field visibility
> is role-aware inside the service. Any authenticated caller can hit it.

### 2.6 AI/ML domain (ai-ml-service — pending)

| Code | Status | Default roles | Used by |
|------|--------|---------------|---------|
| `AI_DASHBOARD` | 🟡 | SUPER_ADMIN, HR_MANAGER, DIRECTOR | View AI insights, attrition risks, anomalies dashboard |

> AI inference endpoints called server-to-server (anomaly check from payroll,
> face verify from attendance) are **not** user-facing — guard them at the
> network/Feign layer, not with permission codes.

### 2.7 Notification domain (notification-service — pending)

Notification listing is always scoped to the caller's own user, so no special
permission code is needed beyond `isAuthenticated()`. Admin-only mass notification
sending is reserved under `SYSTEM_SETTINGS`.

### 2.8 Integration domain (integration-hub — pending)

| Code | Status | Default roles | Used by |
|------|--------|---------------|---------|
| `INTEGRATION_MANAGE` | 🟡 | SUPER_ADMIN, ACCOUNTANT | Trigger 1C sync, generate bank file, view sync log |
| `SYSTEM_SETTINGS` | ✅ | SUPER_ADMIN | `/v1/settings/**` (currently lives in integration-hub per gateway routing) |

### 2.9 System / admin

| Code | Status | Default roles | Used by |
|------|--------|---------------|---------|
| `SYSTEM_USERS` | ✅ | SUPER_ADMIN | User CRUD, link/unlink user↔employee, password reset by admin |
| `SYSTEM_SETTINGS` | ✅ | SUPER_ADMIN | Company-wide settings (`/v1/settings/**`) |
| `SYSTEM_ROLES` | 🟡 | SUPER_ADMIN | Role↔permission edit (currently bundled under `SYSTEM_USERS`) |
| `SYSTEM_AUDIT` | 🟡 | SUPER_ADMIN, DIRECTOR | View `hrms_user.audit_logs` |

---

## 3. Default role → permission matrix

This is the canonical seed for `hrms_user.role_permissions`. Migration `V*__seed_permissions.sql`
in user-service is the single seed point.

```
SUPER_ADMIN       → ALL codes from §2.1–2.9
DIRECTOR          → EMPLOYEE_VIEW_ALL, EMPLOYEE_READ, EMPLOYEE_SALARY_VIEW,
                    ATTENDANCE_VIEW_ALL, PAYROLL_VIEW,
                    REPORT_EXECUTIVE, REPORT_PAYROLL, REPORT_HR,
                    AI_DASHBOARD, SYSTEM_AUDIT
HR_MANAGER        → all EMPLOYEE_*, all ATTENDANCE_*, all LEAVE_*,
                    all PAYROLL_*, all PAYSLIP_*, all REPORT_*,
                    DEPT_MANAGE, AI_DASHBOARD
HR_SPECIALIST     → EMPLOYEE_CREATE/READ/UPDATE/VIEW_ALL/DOCUMENTS/BIOMETRIC,
                    ATTENDANCE_VIEW_ALL, ATTENDANCE_MANAGE,
                    LEAVE_APPROVE_ALL,
                    REPORT_ATTENDANCE, REPORT_LEAVE, REPORT_HR
ACCOUNTANT        → EMPLOYEE_VIEW_ALL, EMPLOYEE_SALARY_VIEW, PAYROLL_VIEW,
                    PAYROLL_READ_ALL, PAYROLL_PROCESS, PAYROLL_ADJUST,
                    PAYROLL_PAY, PAYSLIP_ADJUST,
                    REPORT_PAYROLL, INTEGRATION_MANAGE
MANAGER           → EMPLOYEE_VIEW_TEAM, ATTENDANCE_VIEW_TEAM,
                    LEAVE_APPROVE_TEAM, ATTENDANCE_CHECKIN,
                    LEAVE_REQUEST_OWN, PAYSLIP_VIEW_OWN, EMPLOYEE_VIEW_OWN
TEAM_LEAD         → same as MANAGER but scoped to direct reports only
EMPLOYEE          → ATTENDANCE_CHECKIN, LEAVE_REQUEST_OWN,
                    PAYSLIP_VIEW_OWN, EMPLOYEE_VIEW_OWN
```

`SUPER_ADMIN` is the only role that gets new permissions automatically when
they're added — every other role is opt-in via migration.

---

## 4. How to add a new permission

1. Add a row in §2.x of this file (status 🟡 if not yet implemented).
2. Update §3 if any non-`SUPER_ADMIN` role should get it by default.
3. Add a Flyway migration in `services/user-service/src/main/resources/db/migration/`
   that inserts the code into `hrms_user.permissions` and grants it in
   `hrms_user.role_permissions` for the listed roles.
4. Add the `@PreAuthorize("hasAuthority('CODE')")` to the controller method.
5. Reference the code in `docs/API_CONTRACT.md` next to the endpoint.

---

## 5. JWT claims

The access token carries:

```json
{
  "sub":         "<userId UUID>",
  "email":       "user@example.com",
  "role":        "HR_MANAGER",
  "authorities": ["EMPLOYEE_VIEW_ALL", "PAYROLL_PROCESS", "..."],
  "employeeId":  "<UUID or null>",
  "iat":         1700000000,
  "exp":         1700000900
}
```

Downstream services trust the gateway's signature and read `authorities`
directly into Spring Security `GrantedAuthority`. Re-checking against the
database is **only** done in user-service on login/refresh.
