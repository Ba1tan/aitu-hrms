# Integration Hub

**Port:** 8089 | **Schema:** hrms_integration | **Owner:** Nursultan

## Responsibility
1C:Enterprise OData sync (payroll data → Form 200.00), bank payment file generation, company settings management. Retry with circuit breaker on 1C failures.

## Tables
- `sync_jobs` — tracks each sync attempt: status (PENDING→IN_PROGRESS→SUCCESS/FAILED/RETRYING), payload JSONB, retry_count, onec_document_id
- `company_settings` — key-value config (company.name, company.bin, integration.1c_base_url, etc.)

## Endpoints (9)

```
# 1C Sync
POST /v1/integration/sync/{periodId}              Manual trigger → fetches payroll data → sends to 1C
GET  /v1/integration/sync/status/{jobId}          Check sync job status
GET  /v1/integration/sync/history                 List all sync jobs (?target=&status=&page=)
POST /v1/integration/retry/{jobId}                Retry failed sync

# Bank File
GET  /v1/integration/bank-file/{periodId}         Download bank payment file (MT940/local format)

# Settings (SYSTEM_SETTINGS for writes; setup-status is auth-only)
GET  /v1/settings                                 All company settings (?category=)
PUT  /v1/settings/{key}                           Update setting {value}
GET  /v1/settings/setup-status                    Auth-only — returns {configured, missingRequired[], totalRequired}
POST /v1/settings/complete-setup                  SYSTEM_SETTINGS — marks setup.completed=true after wizard finishes
```

## 1C Sync Flow
```
1. PayrollPeriodApprovedEvent consumed from RabbitMQ
   (or manual trigger via POST /sync/{periodId})
2. Fetch payroll data from payroll-service (Feign)
3. Fetch employee IINs from employee-service (Feign)
4. Build 1C JSON payload (see HRMS_ENTERPRISE_ARCHITECTURE.md §7.2)
5. POST to 1C HTTP Service: {1c_base_url}/hs/hrms/payroll/sync
   - Basic auth (1c_username, 1c_password from settings)
6. On 200 → status=SUCCESS, save onec_document_id
7. On failure → status=FAILED, increment retry_count
   - If retry_count < max_retries → schedule next_retry_at with exponential backoff
   - Publish IntegrationSyncFailedEvent → notification-service alerts HR

Circuit breaker (Resilience4j):
- 10-request sliding window, 50% failure rate → OPEN
- 60s wait in OPEN → HALF_OPEN (3 test calls)
- Retry: 3 attempts, 10s/20s/40s exponential backoff
```

## Events Consumed
- `PayrollPeriodApprovedEvent` → auto-trigger 1C sync
- `PayrollJobCompletedEvent` → optionally auto-trigger if auto_sync enabled

## Events Published
- `IntegrationSyncCompletedEvent` {jobId, periodId, onecDocumentId}
- `IntegrationSyncFailedEvent` {jobId, periodId, errorMessage, retryCount}

## Feign Clients
- `payroll-service` → full payroll data for period (payslips with all tax fields)
- `employee-service` → employee IINs and names for 1C payload

## First-start configuration (setup wizard backend)

When a fresh instance comes up, Flyway seeds the SUPER_ADMIN account
(`user-service` → `V3__seed_super_admin.sql`) and KZ holidays
(`attendance-service` → `V2__seed_holidays_2026.sql`), but no company-specific
data exists. The frontend's `/setup` wizard (see `frontend/hrms-web/AGENTS.md`)
walks the SUPER_ADMIN through the keys below; integration-hub serves them.

### Required setting keys

These keys MUST be set before `setup.completed=true` is allowed. Any missing
required key keeps `GET /v1/settings/setup-status` returning `configured:false`.

| Key | Type | Default | Description |
|---|---|---|---|
| `company.name` | string | — | Legal company name (Russian/Kazakh allowed) |
| `company.bin` | string(12) | — | KZ Business Identification Number — 12 digits |
| `company.legal_address` | string | — | Legal address for tax forms |
| `company.timezone` | string | `Asia/Almaty` | IANA tz name |
| `company.currency` | string | `KZT` | ISO 4217 — only `KZT` supported in v1 |
| `company.locale_default` | enum | `ru` | `ru` \| `kk` \| `en` |
| `company.tax_resident` | bool | `true` | Affects IPN rate (10% vs 20%) for the company itself |
| `attendance.check_in_methods` | array | `["WEB","FACE"]` | Subset of `WEB`, `FACE`, `MANUAL`, `MOBILE` |
| `attendance.require_face` | bool | `false` | If true, manual/web check-in disabled |
| `attendance.work_schedule_default_id` | UUID | — | FK into `hrms_attendance.work_schedules` (created in step 3) |

### Optional setting keys (skippable in wizard, editable later)

| Key | Type | Default | Description |
|---|---|---|---|
| `payroll.payslip_release_day` | int(1-28) | `5` | Day of month payslips become visible to employees |
| `leave.annual_carryover_max_pct` | int(0-100) | `50` | See `services/leave-service/LEAVE_SERVICE.md` carryover rule |
| `integration.1c_base_url` | string | — | If empty, 1C sync is disabled |
| `integration.1c_username` | string | — | Plain |
| `integration.1c_password` | string | — | **Encrypted at rest** with `JWT_SECRET`-derived key |
| `integration.bank_default_format` | enum | `KASPI_TSV` | `KASPI_TSV` \| `HALYK_MT940` \| `JUSAN_CSV` |
| `setup.completed` | bool | `false` | Flipped to `true` by `POST /v1/settings/complete-setup` |

### `GET /v1/settings/setup-status`

```json
{
  "configured":      false,
  "totalRequired":   10,
  "missingRequired": ["company.bin", "company.legal_address", "attendance.work_schedule_default_id"],
  "explicitlyCompleted": false   // mirrors setup.completed
}
```

- `configured` is `true` iff every required key is set AND `setup.completed=true`.
- The endpoint is **authenticated but not permission-gated** — any logged-in
  user can read it so the frontend can decide whether to redirect to `/setup`
  vs show the normal app shell.
- The endpoint must NOT leak setting values, only key names. Values still
  require `SYSTEM_SETTINGS` via `GET /v1/settings`.

### `POST /v1/settings/complete-setup`

- Permission: `SYSTEM_SETTINGS`.
- Validates all required keys are non-empty; returns 409 with
  `{missingRequired:[...]}` if not.
- Sets `setup.completed=true` and writes an audit-log entry
  `event=SETUP_COMPLETED, actor=<userId>`.
- Idempotent — calling it again is a no-op.

### Encrypted-at-rest values

Settings whose key starts with `integration.1c_password` or ends with
`_password`/`_token`/`_secret` are encrypted with AES-256-GCM using a key
derived from `JWT_SECRET` (HKDF-SHA256). The DB row stores `value` as the
ciphertext + nonce + tag; reads decrypt transparently. Never return the
plaintext via `GET /v1/settings` — return `"********"` instead, with the
real value only used internally for outbound calls.

### Audit

Every `PUT /v1/settings/{key}` writes one row to `hrms_user.audit_logs` with
the old/new values (passwords redacted). `setup.completed` flip is also
audited. No event published — settings change is a one-time admin action,
not a workflow.
