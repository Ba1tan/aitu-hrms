# Integration Hub.md

**Port:** 8089 | **Schema:** hrms_integration | **Owner:** Nursultan

## Responsibility
1C:Enterprise OData sync (payroll data → Form 200.00), bank payment file generation, company settings management. Retry with circuit breaker on 1C failures.

## Tables
- `sync_jobs` — tracks each sync attempt: status (PENDING→IN_PROGRESS→SUCCESS/FAILED/RETRYING), payload JSONB, retry_count, onec_document_id
- `company_settings` — key-value config (company.name, company.bin, integration.1c_base_url, etc.)

## Endpoints (7)

```
# 1C Sync
POST /v1/integration/sync/{periodId}              Manual trigger → fetches payroll data → sends to 1C
GET  /v1/integration/sync/status/{jobId}          Check sync job status
GET  /v1/integration/sync/history                 List all sync jobs (?target=&status=&page=)
POST /v1/integration/retry/{jobId}                Retry failed sync

# Bank File
GET  /v1/integration/bank-file/{periodId}         Download bank payment file (MT940/local format)

# Settings (SYSTEM_SETTINGS)
GET  /v1/settings                                 All company settings (?category=)
PUT  /v1/settings/{key}                           Update setting {value}
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
