# HRMS — Microservices Monorepo

> Project overview & root brain. Each service has its own per-service spec
> at `services/{name}/{SERVICE}.md` (e.g. `services/payroll-service/PAYROLL_SERVICE.md`).

## Project

| Field | Value |
|-------|-------|
| **Name** | HRMS with Automated Payroll & AI |
| **Target** | Kazakhstan SMEs (10–500 employees) |
| **Team** | Nursultan (Tech Lead/DevOps/Payroll/AI), Askar (Employee/Leave/Attendance), Nurbol (Frontend) |
| **Production** | https://hrms.nursnerv.uk |

## Services (10)

| Service | Port | Owner | Status |
|---------|------|-------|--------|
| api-gateway | 8080 | Nursultan | Done |
| user-service | 8081 | Nursultan | Done |
| employee-service | 8082 | Askar | Done |
| attendance-service | 8083 | Askar | Done |
| leave-service | 8084 | Askar | Done |
| payroll-service | 8085 | Nursultan | Done |
| ai-ml-service | 8086 | Nursultan | Building new (Python) |
| reporting-service | 8087 | Nursultan | Building new — owns dashboard endpoint |
| notification-service | 8088 | Askar | Building new |
| integration-hub | 8089 | Nursultan | Building new |

## Shared Infra

- **PostgreSQL 16** — one cluster, schema-per-service
- **Redis 7** — JWT blacklist, caching, rate limiting
- **RabbitMQ 3** — async events between services
- **Docker Compose** — dev/staging/prod

## Repo Structure

```
hrms/
├── README.md                          ← this file
├── docker-compose.yml                 ← dev infra (pg, redis, rabbit)
├── docker-compose.microservices.yml   ← all services
├── services/
│   ├── api-gateway/                   ← Spring Cloud Gateway      (API_GATEWAY.md)
│   ├── user-service/                  ← Auth, RBAC, users         (USER_SERVICE.md)
│   ├── employee-service/              ← Employees, depts, docs    (EMPLOYEE_SERVICE.md)
│   ├── attendance-service/            ← Check-in/out, holidays    (ATTENDANCE_SERVICE.md)
│   ├── leave-service/                 ← Leave types, requests     (LEAVE_SERVICE.md)
│   ├── payroll-service/               ← KZ tax calc, payslips     (PAYROLL_SERVICE.md)
│   ├── ai-ml-service/                 ← Python FastAPI, ML        (AI_ML_SERVICE.md)
│   ├── reporting-service/             ← XLSX/PDF + dashboard      (REPORTING_SERVICE.md)
│   ├── notification-service/          ← DB/email/push             (NOTIFICATION_SERVICE.md)
│   └── integration-hub/               ← 1C sync, bank files       (INTEGRATION_HUB.md)
├── hrms-common/                       ← shared Java library (DTOs, events, JWT)
├── frontend/                          ← Nurbol's domain (React) — frontend/hrms-web/AGENTS.md
├── docs/
│   ├── API_CONTRACT.md                ← full endpoint spec for frontend
│   ├── HRMS_ENTERPRISE_ARCHITECTURE.md ← system shape, infra, data flow
│   ├── PERMISSIONS.md                 ← canonical role / permission catalog ★
│   ├── EVENTS.md                      ← canonical RabbitMQ event catalog ★
│   ├── MIGRATIONS.md                  ← Flyway conventions ★
│   ├── OPERATIONS.md                  ← deploy, observability, backup, runbook ★
│   ├── COMPLIANCE.md                  ← KZ PDPL, retention, biometric consent ★
│   ├── TESTING.md                     ← per-layer test strategy ★
│   └── INTEGRATIONS.md                ← 1C, banks, FCM, SMTP contracts ★
└── .github/workflows/
```

★ = authoritative source of truth for that domain. If a service spec
disagrees, the doc above wins — open a PR to update the service doc.

## Kazakhstan Payroll 2026

```
МРП=4325  МЗП=85000
1. earned = gross × (worked/total)
2. OPV = earned×10% (cap 50×МЗП, skip if pensioner)
3. ВОСМС = earned×2% (cap 20×МЗП)
4. deduction = 30×МРП (residents) +882×МРП(disab3) +5000×МРП(disab1/2)
5. taxable = earned−OPV−ВОСМС−deduction (floor 0)
6. IPN = taxable×10% (resident) or 20% (non-resident)
7. net = earned−OPV−ВОСМС−IPN+allowances−deductions
Employer: SO=(earned−OPV)×5%  SN=earned×6%  ОПВР=earned×3.5%
```

## Git Workflow

- `main` → production
- `develop` → staging  
- `feature/{service}-{desc}` → PR to develop
- Commits: `feat(payroll): add anomaly flagging`

## How to Work on a Service

1. Read this file
2. `cd services/{name}` and read its per-service spec (e.g. `PAYROLL_SERVICE.md`)
3. Skim `docs/PERMISSIONS.md` and `docs/EVENTS.md` for any code/event names you need
4. Start infra: `docker compose up -d postgres redis rabbitmq`
5. Run the service: `cd services/{name} && mvn spring-boot:run`
6. Test: `mvn verify` — see `docs/TESTING.md` for slice/integration conventions

## How to Build a Pending Service (handoff path)

1. Read the service's `services/{name}/{SERVICE}.md` — that's the spec
2. Copy `application.yml.example` → `src/main/resources/application.yml`
3. Add the schema's `V1__init_{name}_schema.sql` per `docs/MIGRATIONS.md`
4. Use existing permission codes from `docs/PERMISSIONS.md` — don't invent new ones
5. Use existing event payloads from `docs/EVENTS.md` — same rule
6. CI is already wired in `.github/workflows/{name}-ci.yml` — it activates as soon as `pom.xml` + `Dockerfile` exist

## Bootstrapping a New Tenant

A fresh deploy has no company config — only seeded SUPER_ADMIN + KZ holidays.
First-start configuration is handled by the frontend `/setup` wizard:

- **Backend contract:** `services/integration-hub/INTEGRATION_HUB.md` "First-start configuration" — required setting keys, `/v1/settings/setup-status`, `/v1/settings/complete-setup`
- **Frontend wizard:** `frontend/hrms-web/AGENTS.md` "First-start setup wizard"
- After login, frontend hits `setup-status`; if `configured=false` and role is SUPER_ADMIN, redirect to `/setup`
