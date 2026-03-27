# HRMS Backend — Technical Documentation

**Project:** Human Resource Management System with Automated Payroll  
**Team:** Nursultan Bukenbayev · Askar Seralinov · Nurbol Sembayev   
**University:** Astana IT University  
**Stack:** Java 17 · Spring Boot 3.2 · PostgreSQL 16 · Redis 7

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Module Structure](#2-module-structure)
3. [Database Schema](#3-database-schema)
4. [API Reference](#4-api-reference)
5. [Kazakhstan Payroll Logic](#5-kazakhstan-payroll-logic)
6. [Security Model](#6-security-model)
7. [CI/CD Pipeline](#7-cicd-pipeline)
8. [Local Development Setup](#8-local-development-setup)
9. [Server Deployment](#9-server-deployment)
10. [GitHub Secrets Setup](#10-github-secrets-setup)

---

## 1. Architecture Overview

The system is a **Modular Monolith** — one deployable Spring Boot JAR, but with clean internal module boundaries that mirror a microservices split. This gives the team lower operational overhead for MVP while preserving the ability to split into services later.

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENTS                                  │
│           React Frontend  ·  Mobile App (future)                 │
└─────────────────────────────┬───────────────────────────────────┘
                              │ HTTPS
┌─────────────────────────────▼───────────────────────────────────┐
│                       NGINX (Reverse Proxy)                      │
│          SSL termination · Rate limiting · Static files          │
└─────────────────────────────┬───────────────────────────────────┘
                              │ HTTP (internal)
┌─────────────────────────────▼───────────────────────────────────┐
│               Spring Boot Application  :8080                     │
│                                                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │   AUTH   │ │EMPLOYEE  │ │ATTENDANCE│ │  LEAVE   │           │
│  │ module   │ │ module   │ │  module  │ │  module  │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│                                                                   │
│  ┌──────────┐ ┌──────────┐  ┌──────────────────────┐           │
│  │ PAYROLL  │ │REPORTING │  │  COMMON (shared)      │           │
│  │ module   │ │  module  │  │  exceptions · audit   │           │
│  └──────────┘ └──────────┘  │  security · response  │           │
│                              └──────────────────────┘           │
└─────────────────────────────┬───────────────────────────────────┘
              ┌───────────────┴───────────────┐
              │                               │
┌─────────────▼──────────┐     ┌─────────────▼──────────┐
│   PostgreSQL 16          │     │       Redis 7           │
│   Primary data store     │     │  Token blacklist        │
│   All persistent data    │     │  Session cache          │
└──────────────────────────┘     └────────────────────────┘
```

### Key Design Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Architecture | Modular Monolith | 3-person team, lower ops overhead, easy to split later |
| Language | Java 17 LTS | Team expertise, enterprise standard in Kazakhstan |
| Framework | Spring Boot 3.2 | Battle-tested, large ecosystem, Spring Security |
| Database | PostgreSQL 16 | ACID compliance critical for financial data |
| Cache | Redis 7 | JWT blacklist, session cache |
| DB Migrations | Flyway | Version-controlled schema, team collaboration |
| Mapping | MapStruct | Compile-time, no reflection overhead |
| API Docs | SpringDoc OpenAPI | Auto-generated Swagger UI |

---

## 2. Module Structure

```
src/main/java/kz/aitu/hrms/
│
├── HrmsApplication.java                 ← Spring Boot entry point
│
├── config/                              ← Cross-cutting configuration
│   ├── SecurityConfig.java              ← Spring Security + CORS
│   ├── JpaConfig.java                   ← Auditing config
│   ├── RedisConfig.java                 ← Redis template config
│   └── OpenApiConfig.java               ← Swagger customization
│
├── common/                              ← Shared across ALL modules
│   ├── audit/
│   │   └── BaseEntity.java              ← id, createdAt, updatedAt, etc.
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ResourceNotFoundException.java
│   │   └── BusinessException.java
│   ├── response/
│   │   └── ApiResponse.java             ← Standard JSON envelope
│   └── security/
│       ├── JwtService.java
│       └── JwtAuthenticationFilter.java
│
├── modules/
│   ├── auth/                            ← Authentication & user management
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── dto/AuthDtos.java
│   │   ├── entity/User.java
│   │   ├── entity/Role.java
│   │   └── repository/UserRepository.java
│   │
│   ├── employee/                        ← Employee CRUD, departments, positions
│   │   ├── controller/EmployeeController.java
│   │   ├── controller/DepartmentController.java
│   │   ├── service/EmployeeService.java
│   │   ├── dto/...
│   │   ├── entity/Employee.java
│   │   ├── entity/Department.java
│   │   ├── entity/Position.java
│   │   └── repository/...
│   │
│   ├── attendance/                      ← Daily clock-in/out, work hours
│   │   ├── controller/AttendanceController.java
│   │   ├── service/AttendanceService.java
│   │   ├── dto/...
│   │   ├── entity/AttendanceRecord.java
│   │   └── repository/...
│   │
│   ├── leave/                           ← Leave requests, approvals, balances
│   │   ├── controller/LeaveController.java
│   │   ├── service/LeaveService.java
│   │   ├── dto/...
│   │   ├── entity/LeaveRequest.java
│   │   ├── entity/LeaveType.java
│   │   ├── entity/LeaveBalance.java
│   │   └── repository/...
│   │
│   ├── payroll/                         ← Payroll processing, payslips
│   │   ├── controller/PayrollController.java
│   │   ├── service/PayrollService.java
│   │   ├── calculator/
│   │   │   ├── KazakhstanPayrollCalculator.java   ← Core KZ tax logic
│   │   │   └── PayrollCalculationResult.java
│   │   ├── dto/...
│   │   ├── entity/PayrollPeriod.java
│   │   ├── entity/Payslip.java
│   │   └── repository/...
│   │
│   └── reporting/                       ← Report generation
│       ├── controller/ReportingController.java
│       ├── service/ReportingService.java
│       └── dto/...
```

---

## 3. Database Schema

### Entity Relationship Diagram

```
USERS ─────────────────────── EMPLOYEES
  │  (1 user = 0..1 employee)      │
  │                                 │
  │                    ┌────────────┤
  │                    │            │
  │               DEPARTMENTS   POSITIONS
  │                    │
  │            ATTENDANCE_RECORDS
  │                    │
  │              (employee_id FK)
  │
  │         LEAVE_REQUESTS ──── LEAVE_TYPES
  │                    │
  │              LEAVE_BALANCES
  │
  │         PAYROLL_PERIODS ──── PAYSLIPS
                                    │
                              (employee_id FK)
```

### Tables Summary

| Table | Purpose | Key Fields |
|-------|---------|-----------|
| `users` | System login accounts | email, password, role, employee_id |
| `employees` | HR records | iin, hire_date, base_salary, status |
| `departments` | Org structure | name, manager_id, parent_id |
| `positions` | Job titles | title, min_salary, max_salary |
| `attendance_records` | Daily attendance | employee_id, work_date, check_in, check_out |
| `leave_types` | Leave categories | name, days_allowed, is_paid |
| `leave_requests` | Leave applications | employee_id, start_date, end_date, status |
| `leave_balances` | Leave entitlements per year | employee_id, leave_type_id, year, used_days |
| `payroll_periods` | Monthly pay runs | year, month, working_days, status |
| `payslips` | Individual payslip per employee per period | all calculated tax values |

---

## 4. API Reference

Full interactive documentation is available at `/api/swagger-ui.html` when the app is running.

### Base URL
- **Local:** `http://localhost:8080/api`
- **Production:** `https://your-domain.kz/api`

### Authentication
All endpoints except `/auth/**` require a JWT Bearer token:
```
Authorization: Bearer <access_token>
```

### Endpoint Summary

#### Auth  `/auth`
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/auth/login` | Public | Login, get tokens |
| POST | `/auth/register` | SUPER_ADMIN | Create user |
| POST | `/auth/refresh` | Public | Refresh access token |
| POST | `/auth/change-password` | Any auth | Change own password |
| POST | `/auth/logout` | Any auth | Invalidate token |

#### Employees  `/employees`
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/employees` | HR_MANAGER+ | List all employees (paginated) |
| POST | `/employees` | HR_MANAGER+ | Create employee |
| GET | `/employees/{id}` | HR_MANAGER+ | Get employee by ID |
| PUT | `/employees/{id}` | HR_MANAGER+ | Update employee |
| DELETE | `/employees/{id}` | SUPER_ADMIN | Soft-delete employee |
| GET | `/employees/me` | EMPLOYEE | Get own profile |
| GET | `/departments` | HR_MANAGER+ | List departments |
| POST | `/departments` | SUPER_ADMIN | Create department |

#### Attendance  `/attendance`
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| POST | `/attendance/check-in` | EMPLOYEE | Clock in |
| POST | `/attendance/check-out` | EMPLOYEE | Clock out |
| GET | `/attendance/me` | EMPLOYEE | Own attendance history |
| GET | `/attendance` | HR_MANAGER+ | All attendance records |
| GET | `/attendance/summary/{employeeId}?month=&year=` | HR_MANAGER+ | Monthly summary |
| PUT | `/attendance/{id}` | HR_MANAGER+ | Manual correction |

#### Leave  `/leave`
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/leave/types` | Any auth | List leave types |
| POST | `/leave/requests` | EMPLOYEE | Submit leave request |
| GET | `/leave/requests/me` | EMPLOYEE | Own requests |
| GET | `/leave/requests` | HR_MANAGER+ | All requests |
| PATCH | `/leave/requests/{id}/approve` | HR_MANAGER+ | Approve request |
| PATCH | `/leave/requests/{id}/reject` | HR_MANAGER+ | Reject request |
| GET | `/leave/balances/me` | EMPLOYEE | Own leave balances |

#### Payroll  `/payroll`
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/payroll/periods` | HR_MANAGER+ | List payroll periods |
| POST | `/payroll/periods` | HR_MANAGER+ | Create payroll period |
| POST | `/payroll/periods/{id}/process` | HR_MANAGER+ | Run payroll for all |
| POST | `/payroll/periods/{id}/approve` | SUPER_ADMIN | Approve payroll run |
| GET | `/payroll/payslips/me` | EMPLOYEE | Own payslips |
| GET | `/payroll/payslips/{id}/pdf` | Any auth (own) | Download payslip PDF |
| GET | `/payroll/payslips` | HR_MANAGER+ | All payslips for period |

#### Reports  `/reports`
| Method | Path | Access | Description |
|--------|------|--------|-------------|
| GET | `/reports/payroll-summary/{periodId}` | HR_MANAGER+ | Payroll summary Excel |
| GET | `/reports/form-200/{year}/{quarter}` | ACCOUNTANT+ | Form 200.00 |
| GET | `/reports/employees` | HR_MANAGER+ | Employee list Excel |
| GET | `/reports/attendance/{employeeId}` | HR_MANAGER+ | Attendance report |

### Standard Response Format

All responses follow this envelope:
```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2025-03-01T10:00:00"
}
```

Error response:
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Invalid email format",
    "salary": "Must be positive"
  },
  "timestamp": "2025-03-01T10:00:00"
}
```

---

## 5. Kazakhstan Payroll Logic

The `KazakhstanPayrollCalculator` implements the full payroll calculation per the Kazakhstan Tax Code.

### Calculation Steps

```
Input: grossSalary, workedDays, totalWorkingDays, isResident, hasDisability

Step 1:  earnedSalary = grossSalary × (workedDays / totalWorkingDays)

Step 2:  OPV = earnedSalary × 10%
         (capped at 50 × MRP = 50 × 3692 = 184,600 KZT)
         (0 if employee is pensioner)

Step 3:  mrpDeduction = 1 MRP = 3,692 KZT  (residents only)

Step 4:  taxableIncome = earnedSalary - OPV - mrpDeduction
         (min 0)

Step 5:  IPN = taxableIncome × 10%  (residents)
              taxableIncome × 20%  (non-residents)

Step 6:  netSalary = earnedSalary - OPV - IPN - otherDeductions + allowances

── Employer contributions (not deducted from employee) ──────────────
Step 7:  SO base = earnedSalary - OPV
         SO = SObase × 3.5%

Step 8:  SN = earnedSalary × 9.5% - SO
```

### Example Calculation (100,000 KZT salary, full month)

| Step | Description | Amount (KZT) |
|------|-------------|-------------|
| Gross salary | Base salary | 100,000 |
| Earned salary | Full month | 100,000 |
| OPV (10%) | Pension contribution | -10,000 |
| MRP deduction | Standard deduction | -3,692 |
| Taxable income | 100,000 - 10,000 - 3,692 | 86,308 |
| IPN (10%) | Income tax | -8,631 |
| **Net salary** | **Take-home** | **81,369** |
| SO (3.5%) | Employer social contribution | 3,150 |
| SN | Employer social tax | 6,350 |

---

## 6. Security Model

### Roles & Permissions

| Role | Employees | Attendance | Leave | Payroll | Reports | User Mgmt |
|------|-----------|-----------|-------|---------|---------|-----------|
| SUPER_ADMIN | Full | Full | Full | Full | Full | Full |
| HR_MANAGER | Full | Full | Approve | Process | Most | Read |
| ACCOUNTANT | Read | Read | Read | Read | Full | None |
| EMPLOYEE | Own only | Own only | Own only | Own payslips | None | None |

### Token Flow

```
Login → access_token (15 min) + refresh_token (7 days)
         │
         ├─ Use access_token on every API call
         │   Authorization: Bearer <token>
         │
         └─ When expired → POST /auth/refresh with refresh_token
                        → new access_token
```

Logout stores the token in Redis blacklist (TTL = remaining token lifetime).

---

## 7. CI/CD Pipeline

```
Developer pushes code
        │
        ▼
┌───────────────┐
│ GitHub Actions │
│                │
│ 1. TEST        │  ← Run on every push/PR
│    - PostgreSQL│    to main & develop
│      test DB   │
│    - mvn test  │
│                │
│ 2. BUILD IMAGE │  ← Only on push (not PR)
│    - Docker    │
│    - Push to   │
│      ghcr.io   │
│                │
│ 3. DEPLOY      │  ← Only main branch
│    - SSH into  │
│      server    │
│    - docker    │
│      pull      │
│    - Health    │
│      check     │
└───────────────┘
```

### Branch Strategy

```
main        ← Production. Only merge from develop after testing.
develop     ← Integration branch. All feature branches merge here.
feature/*   ← Individual feature work (feature/employee-crud, etc.)
fix/*       ← Bug fixes
```

---

## 8. Local Development Setup

### Prerequisites

- Java 17 (recommend [SDKMAN](https://sdkman.io/): `sdk install java 17.0.9-tem`)
- Maven (or use `./mvnw`)
- Docker + Docker Compose

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/your-org/hrms-backend.git
cd hrms-backend

# 2. Start PostgreSQL and Redis
docker compose -f docker/docker-compose.staging.yml up -d

# 3. Copy and configure environment (edit values as needed)
cp .env.example .env

# 4. Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 5. Open Swagger UI
open http://localhost:8080/api/swagger-ui.html
```

### IntelliJ Setup

1. Open project as Maven project
2. Set SDK to Java 17
3. Run configuration: `HrmsApplication` with VM option `-Dspring.profiles.active=dev`
4. Set environment variables from `.env` file

---

## 9. Server Deployment

### Initial Server Setup (one-time)

```bash
# On your Linux server
sudo apt update && sudo apt install -y docker.io docker-compose-plugin

# Create app directory
sudo mkdir -p /opt/hrms
sudo chown $USER:$USER /opt/hrms
cd /opt/hrms

# Copy docker-compose and nginx config from repo
# Create .env from .env.example and fill in values
nano .env

# Pull and start
docker compose -f docker/docker-compose.prod.yml up -d
```

### SSL Certificate (Let's Encrypt)

```bash
sudo apt install certbot
sudo certbot certonly --standalone -d your-domain.kz
# Certificates go to /etc/letsencrypt/live/your-domain.kz/
# Update nginx.conf to point to them
```

### After Initial Setup

All subsequent deploys happen automatically via GitHub Actions on push to `main`.

---

## 10. GitHub Secrets Setup

Go to your GitHub repo → Settings → Secrets and variables → Actions → New repository secret.

| Secret Name | Description |
|-------------|-------------|
| `SERVER_HOST` | Your server's public IP |
| `SERVER_USER` | SSH user (e.g. `ubuntu`) |
| `SERVER_SSH_KEY` | Private SSH key (generate with `ssh-keygen`) |
| `DOMAIN` | Your domain name |

All app-level secrets (DB passwords, JWT secret) live in `.env` on the server — not in GitHub secrets. Only the SSH credentials for deployment go in GitHub.
