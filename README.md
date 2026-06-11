# HRMS — HR Management System with Automated Payroll

A microservices-based HR platform for Kazakhstan SMEs (10–500 employees), covering
employee records, attendance, leave, and fully automated Kazakhstan payroll with
1C and bank-file integration.

**Production:** https://hrms.nursnerv.uk

---

## Table of contents

- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [Prerequisites](#prerequisites)
- [Quick start (local development)](#quick-start-local-development)
- [Building](#building)
- [Running the full stack with Docker](#running-the-full-stack-with-docker)
- [Configuration](#configuration)
- [First-run setup](#first-run-setup)
- [Kazakhstan payroll](#kazakhstan-payroll)
- [Testing](#testing)
- [Deployment](#deployment)
- [Documentation](#documentation)
- [Contributing](#contributing)

---

## Architecture

Nine Spring Boot services behind a single API gateway, plus a React frontend.
Each service owns its own PostgreSQL schema; services communicate synchronously
via the gateway/Feign and asynchronously via RabbitMQ events.

| Service | Port | Responsibility |
|---|---|---|
| api-gateway | 8080 | Routing, auth filter, rate limiting (Spring Cloud Gateway) |
| user-service | 8081 | Authentication, JWT, RBAC, users, audit log |
| employee-service | 8082 | Employees, departments, positions, documents |
| attendance-service | 8083 | Check-in/out, work schedules, holidays, summaries |
| leave-service | 8084 | Leave types, requests, balances, carryover |
| payroll-service | 8085 | KZ tax calculation, payslips, payroll periods |
| reporting-service | 8087 | XLSX/PDF reports, dashboard aggregation |
| notification-service | 8088 | In-app, email, and push notifications |
| integration-hub | 8089 | 1C:Enterprise sync, bank payment files, company settings |

```
            ┌─────────┐     ┌─────────────┐
 client ──▶ │  nginx  │ ──▶ │ api-gateway │ ──▶ 8081..8089 microservices
            └─────────┘     └─────────────┘            │
                                                        ├── PostgreSQL 16 (schema-per-service)
                                                        ├── Redis 7 (JWT blacklist, cache, rate limit)
                                                        └── RabbitMQ 3 (async events)
```

## Tech stack

- **Backend:** Java 17, Spring Boot 3, Spring Cloud Gateway, Spring Security (JWT)
- **Data:** PostgreSQL 16 (one cluster, schema per service), Flyway migrations
- **Messaging / cache:** RabbitMQ 3, Redis 7
- **Frontend:** React + TypeScript (Vite), TanStack Query
- **Build / deploy:** Maven, Docker, Docker Compose, GitHub Actions (CI per service)

## Repository layout

```
hrms-backend/
├── docker-compose.microservices.yml   # full stack (all services + infra)
├── env.example                         # copy to .env and fill in
├── services/
│   ├── hrms-common/                    # shared Java library (DTOs, events, JWT, security)
│   ├── api-gateway/
│   ├── user-service/
│   ├── employee-service/
│   ├── attendance-service/
│   ├── leave-service/
│   ├── payroll-service/
│   ├── reporting-service/
│   ├── notification-service/
│   └── integration-hub/
├── frontend/hrms-web/                  # React web client
├── proxy/nginx/nginx.conf              # reverse proxy (production)
├── bruno/                              # API collection (Bruno) for manual testing
├── docs/                               # architecture & domain documentation
└── .github/workflows/                  # CI pipelines
```

Per-service details — endpoints, permissions, and events — are documented
centrally in the [`docs/`](#documentation) folder (`API_CONTRACT.md`,
`PERMISSIONS.md`, `EVENTS.md`).

## Prerequisites

- **JDK 17**
- **Maven 3.9+**
- **Docker** and **Docker Compose** (for infra and the full stack)
- **Node.js 18+** (for the frontend)

## Quick start (local development)

Run shared infrastructure in Docker and individual services from your IDE/CLI.

```bash
# 1. Clone
git clone https://github.com/<owner>/hrms-backend.git
cd hrms-backend

# 2. Environment
cp env.example .env          # edit secrets (DB, Redis, RabbitMQ, JWT_SECRET)

# 3. Start infrastructure only
docker compose -f docker-compose.microservices.yml up -d postgres redis rabbitmq

# 4. Build the shared library once (required before any service compiles)
cd services/hrms-common && mvn install && cd ../..

# 5. Run a service
cd services/user-service && mvn spring-boot:run
```

Repeat step 5 for any services you need. Start `api-gateway` last; it routes to
the rest. The frontend dev server:

```bash
cd frontend/hrms-web
npm install
npm run dev
```

## Building

`hrms-common` must be installed to your local Maven repository before any
dependent service will compile:

```bash
cd services/hrms-common && mvn install
```

Then build any service:

```bash
cd services/payroll-service && mvn clean package
```

## Running the full stack with Docker

The compose file runs all services plus PostgreSQL, Redis, and RabbitMQ. It pulls
published images by default; build locally with `--build` if you prefer.

```bash
cp env.example .env          # set image tags / secrets / host ports
docker compose -f docker-compose.microservices.yml up -d
```

Published host ports (configurable in `.env`):

- `GATEWAY_PORT` (default `8090`) → api-gateway
- `NGINX_PORT` (default `8092`) → nginx reverse proxy (production profile)
- `FRONTEND_PORT` → frontend (staging only; left empty in production)

## Configuration

All runtime configuration is via environment variables — see **`env.example`**
for the full list. Key ones:

| Variable | Purpose |
|---|---|
| `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL credentials |
| `REDIS_PASSWORD` | Redis auth |
| `RABBITMQ_USER`, `RABBITMQ_PASS` | RabbitMQ auth |
| `JWT_SECRET` | Base64 secret, ≥32 bytes — `openssl rand -base64 32` |
| `JWT_ACCESS_EXPIRY`, `JWT_REFRESH_EXPIRY` | Token lifetimes (ms) |
| `MAIL_*` | SMTP for password reset / notifications |

Secrets such as the 1C password are encrypted at rest (AES-256-GCM, key derived
from `JWT_SECRET`). Never commit a real `.env`.

## First-run setup

A fresh deployment has no company configuration — only a seeded `SUPER_ADMIN`
account and the Kazakhstan public-holiday calendar. After the first login, the
frontend checks `GET /api/v1/settings/setup-status`; if setup is incomplete and
the user is `SUPER_ADMIN`, it redirects to a guided `/setup` wizard that captures
company details, work schedule, attendance methods, and integration settings.

See `docs/API_CONTRACT.md` (settings endpoints) for the required setting keys
and the `/v1/settings/setup-status` / `/v1/settings/complete-setup` contracts.

## Kazakhstan payroll

All monetary calculations use `BigDecimal` (never floating point). 2026 constants:
`МРП = 4325`, `МЗП = 85000`.

```
1. earned   = gross × (worked days / total days)
2. ОПВ      = earned × 10%        (cap 50×МЗП; skipped for pensioners)
3. ВОСМС    = earned × 2%         (cap 20×МЗП)
4. deduction= 30×МРП (residents) + 882×МРП (disability gr.3) + 5000×МРП (gr.1/2)
5. taxable  = earned − ОПВ − ВОСМС − deduction        (floor 0)
6. ИПН      = taxable × 10% (resident) or 20% (non-resident)
7. net      = earned − ОПВ − ВОСМС − ИПН + allowances − deductions

Employer contributions:
  СО  = (earned − ОПВ) × 5%
  СН  = earned × 6%
  ОПВР= earned × 3.5%
```

## Testing

```bash
cd services/{name} && mvn verify
```

Tests run against an in-memory H2 database in PostgreSQL-compatibility mode with
Flyway disabled (schema via `ddl-auto: create-drop`). See `docs/TESTING.md` for
the slice/integration conventions. The `bruno/` collection covers manual,
end-to-end API checks.

## Deployment

Production runs the Docker Compose stack behind a reverse proxy. The recommended
edge is Nginx Proxy Manager (TLS termination) in front of the bundled
`proxy/nginx/nginx.conf`, which routes `/api/*` to the gateway and everything
else to the frontend.

> **Client IP behind the proxy:** the bundled nginx uses the `realip` module to
> recover the real client IP from `X-Forwarded-For`. If you add another proxy
> layer, make sure each hop forwards `X-Forwarded-For` / `X-Real-IP`, otherwise
> audit logs and rate limiting will key off the proxy address.

CI builds and publishes a container image per service on push (see
`.github/workflows/`). See `docs/OPERATIONS.md` for the full deploy, backup, and
observability runbook.

## Documentation

| Document | Contents |
|---|---|
| `docs/HRMS_ENTERPRISE_ARCHITECTURE.md` | System shape, infrastructure, data flow |
| `docs/API_CONTRACT.md` | Full endpoint specification |
| `docs/PERMISSIONS.md` | Roles and permission catalog |
| `docs/EVENTS.md` | RabbitMQ event catalog |
| `docs/MIGRATIONS.md` | Flyway conventions |
| `docs/OPERATIONS.md` | Deploy, observability, backup, runbook |
| `docs/COMPLIANCE.md` | KZ PDPL, data retention |
| `docs/TESTING.md` | Test strategy per layer |
| `docs/INTEGRATIONS.md` | 1C, banks, FCM, SMTP contracts |

## Contributing

- Branches: `main` → production, `develop` → staging, `feature/{service}-{desc}` → PR to `develop`.
- Commit style: Conventional Commits, e.g. `feat(payroll): add anomaly flagging`.
- A service's `pom.xml` + `Dockerfile` activate its CI workflow automatically.
