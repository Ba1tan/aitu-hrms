# HRMS — Claude Code Project Brain
# Source of truth for ALL development. Read this before touching any file.

## Project Identity

**System:** Human Resource Management System with Automated Payroll  
**Target:** Kazakhstan SMEs, 50–500 employees  
**Team:** Nursultan Torekhanov (Tech Lead / DevOps), Askar Seralinov (employee/leave/attendance), Nurbol Sembayev (frontend)  
**Supervisor:** Omirgaliyev Ruslan  
**Production URL:** https://hrms.nursnerv.uk  
**API Base:** https://hrms.nursnerv.uk/api  

---

## Two-Phase Architecture Strategy

### Phase 1 — Modular Monolith (CURRENT — MVP)
Single Spring Boot application. Modules under `kz.aitu.hrms.modules.*`.  
**Rule:** Modules communicate ONLY via `@Service` interfaces, never by injecting another module's `@Repository` directly.  
Module boundaries are designed to extract cleanly into microservices in Phase 2.

### Phase 2 — Microservices (Post-diploma, per ADR-001)
9 independent services: `user`, `employee`, `attendance`, `leave`, `payroll`, `ai-ml` (Python/FastAPI), `reporting`, `notification`, `integration-hub` (1C).  
Communication: REST (synchronous) + RabbitMQ (asynchronous events).  
See `docs/dev-phases/full/` for extraction guides.

---

## Tech Stack (per ADR-002, ADR-003, ADR-004, ADR-005)

| Layer | Technology | ADR |
|---|---|---|
| Language | Java 17 LTS | ADR-002 |
| Framework | Spring Boot 3.3.5 | ADR-003 |
| Security | Spring Security 6 + JWT (access 15min, refresh 7d rotation) | ADR-003 |
| Batch | Spring Batch (payroll processing) | ADR-003 |
| Persistence | Spring Data JPA + Hibernate | ADR-004 |
| DB | PostgreSQL 16 — Flyway migrations, `NUMERIC(15,2)` for money | ADR-004 |
| Cache | Redis 7 — JWT blacklist, employee data cache (24h TTL) | ADR-004 |
| Messaging | RabbitMQ (Phase 2 — for MVP use in-process events) | ADR-006 |
| AI/ML | Python 3.11 + FastAPI + scikit-learn (Phase 2) | ADR-002 |
| Frontend state | Zustand (auth + UI) + React Query (server data) | |
| Frontend UI | React 18 + MUI v5 + React Hook Form + Yup | |
| Build | Maven |  |
| Containers | Docker + Docker Compose | ADR-005 |
| Proxy | Nginx Proxy Manager + Cloudflare | |
| CI/CD | GitHub Actions → GHCR → SSH deploy | |

---

## Package Structure

```
kz.aitu.hrms/
├── HrmsApplication.java
├── common/
│   ├── security/          # JwtFilter, JwtService
│   ├── exception/         # GlobalExceptionHandler, BusinessException, ResourceNotFoundException
│   ├── audit/             # BaseEntity (UUID id, createdAt, updatedAt, createdBy, updatedBy, isDeleted)
│   └── response/          # ApiResponse<T>
├── config/                # SecurityConfig, AppConfig (JPA auditing), OpenApiConfig
└── modules/
    ├── auth/              # ✅ DONE — login, register, refresh, logout, change-password
    ├── employee/          # ✅ DONE — Employee, Department, Position CRUD + search
    ├── payroll/           # ✅ DONE — Periods, KZ tax calculation, payslip generation, approval workflow
    ├── leave/             # 🔲 TODO — see docs/dev-phases/mvp/leave.md
    ├── attendance/        # 🔲 TODO — see docs/dev-phases/mvp/attendance.md
    ├── notification/      # 🔲 TODO — see docs/dev-phases/mvp/notification.md
    └── reporting/         # 🔲 TODO — see docs/dev-phases/mvp/reporting.md
```

Each module follows this internal structure:
```
modules/{name}/
├── controller/    # @RestController — HTTP, @PreAuthorize, delegates to service
├── service/       # Interface + Impl — all business logic, @Transactional
├── repository/    # @Repository — Spring Data JPA only
├── entity/        # @Entity — extends BaseEntity
├── dto/           # request/ and response/ subpackages (or single DTOs class)
├── enums/         # module-specific enums
└── mapper/        # MapStruct (optional, can map manually for MVP)
```

---

## Roles & RBAC (per Assignment 1 + V1 schema)

```java
public enum Role {
    SUPER_ADMIN,     // Full system access
    HR_MANAGER,      // HR operations, payroll processing, leave approval (final)
    ACCOUNTANT,      // View payroll, generate reports, mark periods as paid
    MANAGER,         // Approve/reject leave for their team  ← V3 migration adds this
    EMPLOYEE         // Self-service: payslips, leave requests, attendance
}
```

**IMPORTANT:** V1 schema has `CHECK (role IN ('SUPER_ADMIN','HR_MANAGER','ACCOUNTANT','EMPLOYEE'))`.  
**V3 migration** must ALTER the constraint to add `'MANAGER'` before using this role.  
See `docs/dev-phases/mvp/V3-add-manager-role.sql`.

Role → Endpoint access:
- `SUPER_ADMIN` — everything
- `HR_MANAGER` — employee CRUD, payroll generate/approve, leave final approval, all reports
- `ACCOUNTANT` — read payroll, mark as paid, generate reports
- `MANAGER` — read team employees, approve/reject leave requests for direct reports
- `EMPLOYEE` — own payslips, submit leave requests, check-in/check-out

---

## Domain Rules & Invariants

### Kazakhstan Payroll Calculation (per KazakhstanPayrollCalculator.java — DO NOT REORDER)
```
Step 1: Earned salary = gross × (workedDays / totalDays)   — prorate if partial month
Step 2: OPV = earned × 10%  capped at 50×MRP              — ОПВ pension, skip if pensioner
Step 3: OOPV = earned × 1.5%                               — ООПВ (if applicable, 0 for most)
Step 4: MRP deduction = 1×MRP (residents), 0 (non-residents), +882×MRP (disability)
Step 5: Taxable = earned − OPV − MRP_deduction            — floored at 0
Step 6: IPN = taxable × 10% (residents) or × 20% (non-residents)  — ИПН income tax
Step 7: Net = earned − OPV − OOPV − IPN + allowances − otherDeductions  — take-home pay
Step 8: SO = (earned − OPV) × 3.5%                        — СО employer social, NOT deducted
Step 9: SN = earned × 9.5% − SO                           — СН employer social tax, NOT deducted
```
**Constants (application.yml, 2024):** `mrp=3692`, `min-wage=85000`  
**ALL monetary values:** `BigDecimal` with `NUMERIC(15,2)`. Never `double`/`float`.

### Leave Rules (per Assignment 1 + Kazakhstan Labour Code Art. 88)
- Annual leave: minimum 24 working days (already seeded in `leave_types`)
- `leave_requests` uses FK to `leave_types` table — NOT an enum
- Balance validation: `entitled_days - used_days >= days_requested` BEFORE inserting request
- Leave + balance update must be atomic: single `@Transactional` spanning both tables
- Sequence Diagram 2: cache employee data (manager_id) in Redis with 24h TTL

### Attendance Rules (per Sequence Diagram 3)
- One record per employee per `work_date` (UNIQUE constraint in DB)
- `worked_hours` is `NUMERIC(5,2)` — compute as `EXTRACT(EPOCH FROM (check_out - check_in)) / 3600`
- LATE if check_in > 09:00 + configurable threshold (default 10 min) from `application.yml`
- HALF_DAY if `worked_hours < 4`
- HOLIDAY / WEEKEND statuses set programmatically, not by check-in
- Fraud detection (score column) is Phase 2 only — skip for MVP

---

## Database Conventions

- All PKs: `UUID PRIMARY KEY DEFAULT gen_random_uuid()` — never SERIAL
- All entities extend `BaseEntity` — never re-declare id/createdAt/updatedAt/createdBy/updatedBy
- `is_deleted` soft delete pattern — all queries must filter `deleted = false`
- Enum columns: `VARCHAR(50) NOT NULL` — never PostgreSQL native ENUM type
- Money: `NUMERIC(15,2)` — never DECIMAL without precision
- Flyway naming: `V{n}__{description}.sql` — next after V2 is **V3**
- FK naming: `fk_{table}_{referenced}`, Index naming: `idx_{table}_{columns}`
- Migrations are **irreversible in production** — never edit applied migrations

### Current Schema (V1 + V2)
Tables already created: `users`, `departments`, `positions`, `employees`, `attendance_records`,  
`leave_types` (seeded), `leave_requests`, `leave_balances`, `payroll_periods`, `payslips`  
**No new migrations needed for leave or attendance — tables exist, map entities to them.**

---

## API Conventions

- **Context path:** `/api` (server.servlet.context-path)  
- **Controller paths:** `/v1/{module}` → effective URL: `/api/v1/{module}`  
- **Auth controller:** `/auth/**` → effective URL: `/api/auth/**`  
- All responses: `ApiResponse<T> { success, message, data, timestamp }`  
- Pagination: `Page<T>` via `Pageable` — default `page=0`, `size=20`  
- File downloads: `ResponseEntity<byte[]>` with `Content-Disposition: attachment; filename=...`  
- Status codes: 201 for creates, 200 for updates/reads, 204 for deletes (use `noContent()`)

---

## Cross-Module Calls (MVP — in-process only)

```
payroll  → employee    : EmployeeService.findActiveEmployees()
payroll  → attendance  : AttendanceService.getMonthlyHours(employeeId, month, year)  [Phase 1.5]
leave    → employee    : EmployeeService.getById(id)  — to resolve manager
leave    → notification: NotificationService.notify(...)
attendance → notification: NotificationService.notify(...)
payroll  → notification: NotificationService.notify(...)
reporting → payroll   : PayslipRepository (reporting reads payslip data)
reporting → employee  : EmployeeService.getAllWithDetails()
```

When extracting to microservices: each `→` becomes a Feign client or RabbitMQ event subscription.

---

## Redis Usage Patterns (per Sequence Diagrams)

| Data | Key Pattern | TTL | Used by |
|------|------------|-----|---------|
| JWT blacklist | `jwt:blacklist:{token}` | 7d | AuthService |
| Employee data | `employee:{id}` | 24h | Leave module (manager_id lookup) |
| Fingerprint hash | `biometric:{hash}` | 1h | Attendance (Phase 2) |
| Daily checkin counter | `checkins:{date}` | 24h | Attendance (Phase 2) |

Always wrap Redis calls in try-catch — degrade gracefully if Redis is unavailable.

---

## Security Config Notes

Spring Security matches paths **after** context-path stripping:
- `PUBLIC_ENDPOINTS` uses `/auth/**` not `/api/auth/**` — this is correct
- All `/v1/**` endpoints require authentication unless explicitly added to PUBLIC_ENDPOINTS

---

## Important Files

| File | Purpose |
|------|---------|
| `src/main/resources/application.yml` | All config — never hardcode values |
| `src/main/resources/db/migration/` | Flyway scripts — V1 (schema), V2 (seed admin), V3+ (additions) |
| `docker-compose.yml` (or `docker/`) | app + postgres + redis |
| `.github/workflows/ci-cd.yml` | CI/CD pipeline |
| `docs/dev-phases/mvp/` | **Read before implementing any MVP backend module** |
| `docs/dev-phases/full/frontend.md` | **React Query + Zustand frontend guide** |
| `docs/dev-phases/full/microservices-extraction.md` | Phase 2 microservice work |
| `.claude/commands/` | Slash commands for common operations |

---

## Before Starting Any Task

1. Identify the module — read its existing code first (`find src -name "*.java" | grep {module}`)
2. Check which Flyway migration version is next (`ls src/main/resources/db/migration/`)
3. For MVP module work → read the relevant `docs/dev-phases/mvp/{module}.md`
4. For Phase 2 work → read `docs/dev-phases/full/`
5. Always run `mvn compile` after changes before committing
