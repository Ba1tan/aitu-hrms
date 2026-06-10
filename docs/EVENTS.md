# EVENTS — Canonical Catalog

**Status:** authoritative source of truth for RabbitMQ event payloads,
routing keys, and producer/consumer wiring. If any other doc disagrees with
this file, **this file wins**.

---

## 1. Topology

- **Broker:** RabbitMQ 3 (single cluster, shared by all services).
- **Exchange:** `hrms.events` — type `topic`, durable, non-auto-delete.
  Every service binds its consumer queues to this single exchange.
- **Connection:** services autowire `RabbitTemplate` and a per-service
  `RabbitConfig` declares the exchange + bindings.
- **Resilience:** every publisher catches broker exceptions and **logs
  + drops** (does not block the originating request). The consequence is
  that consumers must be idempotent — and downstream sync paths (e.g. payroll
  totals) cannot rely on these events being eventually delivered.

### Queue naming convention

```
{consumer-service}.{event-stream-without-prefix}
```

Example: leave-service consuming `employee.created` → queue named
`leave.employee.created`. This makes broker UI auditing straightforward.

### Routing key convention

```
{producer-domain}.{noun}.{verb-past}
```

Example: `payroll.job.completed`, `leave.approved`. Use lowercase,
dot-separated, past-tense verbs. Never include the producer service name in
the routing key — domain ownership is what matters.

---

## 2. Event catalog

Each row lists: routing key, payload class, publisher, every consumer.
Status:
- ✅ **Live** — both publisher and at least one consumer wired in code today
- 🟡 **Reserved** — payload exists or is documented; no consumer wired yet
- 🔴 **Out-of-scope** — referenced in older docs but explicitly removed

| Routing key | Payload | Publisher | Consumers | Status |
|---|---|---|---|---|
| `employee.created` | `EmployeeCreatedEvent` | employee-service | user-service (`Q_EMPLOYEE_CREATED`), leave-service (`leave.employee.created`), payroll-service (`payroll.employee.created`), notification-service (`notification.employee.created`) | ✅ |
| `employee.terminated` | `EmployeeTerminatedEvent` | employee-service | notification-service (`notification.employee.terminated`) | ✅ |
| `employee.salary.changed` | `SalaryChangedEvent` | employee-service | payroll-service (`payroll.employee.salary.changed`), notification-service | ✅ |
| `attendance.recorded` | `AttendanceRecordedEvent` | attendance-service | notification-service (late-arrival nudge) | ✅ |
| `leave.request.created` | `LeaveRequestCreatedEvent` | leave-service | notification-service (notify approver) | ✅ |
| `leave.approved` | `LeaveApprovedEvent` | leave-service | attendance-service (`attendance.leave.approved`) auto-marks ON_LEAVE; notification-service (notify employee) | ✅ |
| `leave.rejected` | `LeaveRejectedEvent` | leave-service | notification-service (notify employee) | ✅ |
| `payroll.job.started` | `PayrollJobStartedEvent` | payroll-service | notification-service (HR progress notice) | ✅ |
| `payroll.job.completed` | `PayrollJobCompletedEvent` | payroll-service | reporting-service (dashboard cache), notification-service (HR notice) | ✅ |
| `payroll.period.approved` | `PayrollPeriodApprovedEvent` | payroll-service | integration-hub (1C sync trigger), notification-service (per-employee payslip-ready) | ✅ |
| `user.account.created` | `UserAccountCreatedEvent` | user-service | notification-service (welcome email + temp password) | ✅ |
| `user.password.reset-requested` | `PasswordResetRequestedEvent` | user-service | notification-service (reset link email) | ✅ |
| `audit.recorded` | `AuditEvent` (hrms-common) | employee-, attendance-, leave-, payroll-service, integration-hub | user-service (`user.audit.recorded`) → persists to `hrms_user.audit_logs` | ✅ |

### Integration sync events (published by integration-hub)

| Routing key | When emitted | Producer | Consumers | Status |
|---|---|---|---|---|
| `integration.1c.synced` | 1C sync job succeeded | integration-hub (`IntegrationEventPublisher.publishCompleted`) | none wired yet 🟡 | ✅ (publisher only) |
| `integration.sync.failed` | 1C sync job failed (after retries) | integration-hub (`IntegrationEventPublisher.publishFailed`) | none wired yet 🟡 | ✅ (publisher only) |

### Reserved events (names agreed, not published yet)

| Routing key | When emitted | Producer | Notes |
|---|---|---|---|
| `payslip.adjusted` 🟡 | Per-employee correction inside an approved period | payroll-service | Reuse `PayslipAdjustedEvent` shape — payslipId, employeeId, periodId, correctionAmount |
| `report.generated` 🟡 | Long-running XLSX/PDF finished | reporting-service | Allows notification-service to push "Your report is ready" |
| `notification.sent` 🟡 | Outbound email/push delivered | notification-service | Optional — only if reporting wants delivery analytics |

---

## 3. Payload schemas

### `EmployeeCreatedEvent`
```json
{
  "employeeId": "UUID",
  "fullName":   "string",
  "email":      "string|null",
  "salary":     "decimal",
  "departmentId": "UUID|null"
}
```

### `EmployeeTerminatedEvent`
```json
{
  "employeeId":      "UUID",
  "terminationDate": "ISO date",
  "reason":          "string"
}
```

### `SalaryChangedEvent`
```json
{
  "employeeId":     "UUID",
  "previousSalary": "decimal",
  "newSalary":      "decimal",
  "effectiveDate":  "ISO date",
  "reason":         "string",
  "approvedBy":     "UUID"      // present in hrms-common variant only — see §6
}
```

### `AttendanceRecordedEvent`
```json
{
  "recordId":     "UUID",
  "employeeId":   "UUID",
  "workDate":     "ISO date",
  "status":       "PRESENT|LATE|ABSENT|HALF_DAY|ON_LEAVE|HOLIDAY|WEEKEND",
  "method":       "FACE|MANUAL|WEB|MOBILE",
  "checkIn":      "ISO datetime|null",
  "checkOut":     "ISO datetime|null",
  "workedHours":  "decimal|null"
}
```

### `LeaveRequestCreatedEvent`
```json
{
  "requestId":    "UUID",
  "employeeId":   "UUID",
  "managerId":    "UUID|null",
  "leaveType":    "string (e.g. ANNUAL, SICK)",
  "startDate":    "ISO date",
  "endDate":      "ISO date",
  "daysRequested": "int"
}
```

### `LeaveApprovedEvent`
```json
{
  "requestId":  "UUID",
  "employeeId": "UUID",
  "startDate":  "ISO date",
  "endDate":    "ISO date",
  "leaveType":  "string"
}
```

### `LeaveRejectedEvent`
```json
{
  "requestId":  "UUID",
  "employeeId": "UUID",
  "reviewedBy": "UUID",
  "comment":    "string"
}
```

### `PayrollJobStartedEvent`
```json
{
  "periodId":      "UUID",
  "year":          "int",
  "month":         "int (1-12)",
  "employeeCount": "int",
  "startedAt":     "ISO datetime",
  "startedBy":     "UUID"
}
```

### `PayrollJobCompletedEvent`
```json
{
  "periodId":      "UUID",
  "employeeCount": "int",
  "totalGross":    "decimal",
  "totalNet":      "decimal",
  "completedAt":   "ISO datetime"
}
```

### `PayrollAnomalyDetectedEvent`
```json
{
  "payslipId":    "UUID",
  "employeeId":   "UUID",
  "periodId":     "UUID",
  "anomalyScore": "decimal 0..1",
  "flags":        ["string"]
}
```

### `PayrollPeriodApprovedEvent`
```json
{
  "periodId":     "UUID",
  "year":         "int",
  "month":        "int (1-12)",
  "payslipCount": "long",
  "totalNet":     "decimal",
  "approvedAt":   "ISO datetime",
  "approvedBy":   "UUID"
}
```

### `UserAccountCreatedEvent`
```json
{
  "userId":            "UUID",
  "email":             "string",
  "firstName":         "string",
  "lastName":          "string",
  "role":              "string (role code from PERMISSIONS.md §1)",
  "employeeId":        "UUID|null",
  "temporaryPassword": "string",
  "createdAt":         "ISO datetime"
}
```

### `PasswordResetRequestedEvent`
```json
{
  "userId":     "UUID",
  "email":      "string",
  "firstName":  "string",
  "resetToken": "string (opaque)",
  "ttlSeconds": "long"
}
```

### `AuditEvent`

System-wide audit trail. Any service emits this on a sensitive write; user-service
consumes it into `hrms_user.audit_logs` (the single table the admin audit-log
endpoint reads). Built via `kz.aitu.hrms.common.audit.AuditEvents.build(...)`,
which pulls the actor from the `AuthenticatedUser` principal and IP/user-agent
from the current request. user-service's own actions are written directly (no
round-trip). `oldValue`/`newValue` are pre-serialized JSON strings.

```json
{
  "actorId":       "UUID|null",
  "actorEmail":    "string|null",
  "action":        "CREATE|UPDATE|DELETE|APPROVE|REJECT|CANCEL|TERMINATE|SALARY_CHANGE|PROCESS|PAY|LOCK|ADJUST|RECALCULATE|BULK_ABSENT|CARRYOVER|SYNC|RETRY|BANK_FILE|SETUP_COMPLETED",
  "entityType":    "EMPLOYEE|DEPARTMENT|POSITION|ATTENDANCE|HOLIDAY|WORK_SCHEDULE|LEAVE_REQUEST|LEAVE_TYPE|LEAVE_BALANCE|PAYROLL_PERIOD|PAYSLIP|PAYROLL_ADDITION|SETTING|SYNC_JOB|BANK_FILE|EMPLOYEE_DOCUMENT",
  "entityId":      "UUID|null",
  "oldValue":      "string(JSON)|null",
  "newValue":      "string(JSON)|null",
  "ipAddress":     "string|null",
  "userAgent":     "string|null",
  "sourceService": "string (e.g. employee-service)",
  "occurredAt":    "ISO datetime"
}
```

---

## 4. Consumer queues currently bound

| Queue | Consumer service | Routing key |
|---|---|---|
| `user.employee.created` | user-service | `employee.created` |
| `payroll.employee.created` | payroll-service | `employee.created` |
| `payroll.employee.salary.changed` | payroll-service | `employee.salary.changed` |
| `leave.employee.created` | leave-service | `employee.created` |
| `attendance.leave.approved` | attendance-service | `leave.approved` |
| `user.audit.recorded` | user-service | `audit.recorded` |
| `integration.payroll.period.approved` | integration-hub | `payroll.period.approved` |
| `integration.payroll.job.completed` | integration-hub | `payroll.job.completed` |
| `reporting.payroll.job.completed` | reporting-service | `payroll.job.completed` |
| `notification.employee.created` | notification-service | `employee.created` |
| `notification.employee.terminated` | notification-service | `employee.terminated` |
| `notification.employee.salary.changed` | notification-service | `employee.salary.changed` |
| `notification.attendance.recorded` | notification-service | `attendance.recorded` |
| `notification.leave.request.created` | notification-service | `leave.request.created` |
| `notification.leave.approved` | notification-service | `leave.approved` |
| `notification.leave.rejected` | notification-service | `leave.rejected` |
| `notification.payroll.job.started` | notification-service | `payroll.job.started` |
| `notification.payroll.job.completed` | notification-service | `payroll.job.completed` |
| `notification.payroll.period.approved` | notification-service | `payroll.period.approved` |
| `notification.user.account.created` | notification-service | `user.account.created` |
| `notification.user.password.reset-requested` | notification-service | `user.password.reset-requested` |

New consumers should declare their queues in their own `RabbitConfig`
following the same naming convention. See §1 for the convention.

---

## 5. How to add a new event

1. Add the payload class in `services/hrms-common/src/main/java/kz/aitu/hrms/common/event/`.
   Use Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.
2. Add the routing-key constant `RK_<NAME>` in the producer service's `RabbitConfig`.
3. Wire `EventPublisher.publish(...)` and call from inside the transaction's
   commit hook (use `TransactionSynchronizationManager` so the event is only
   sent if the DB write succeeds).
4. For each consumer service, add the queue constant `QUEUE_<NAME>`, declare
   the `Queue` and `Binding` beans, write a `@RabbitListener` method.
5. Add a row to §2 of this file.

---

## 6. Known inconsistencies / migration debt

- **Two `SalaryChangedEvent` classes** — one in `hrms-common.event`, one in
  `employee-service.event`. employee-service currently publishes the local
  variant (without `approvedBy`). Consolidate to the hrms-common variant and
  delete the local one.
- **Consumers are now wired** — notification-service consumes 11 events
  (employee created/terminated/salary, leave created/approved/rejected,
  payroll started/completed/period-approved, user account-created/password-reset,
  attendance recorded); reporting-service consumes `payroll.job.completed`;
  integration-hub consumes `payroll.period.approved` + `payroll.job.completed`.
  The earlier "🟡 / publish-only / no-op downstream" notes are obsolete — the
  table in §2 reflects the current wiring.
- **No DLQ / retry policy declared** — every queue is plain durable, and every
  consumer swallows-and-drops on failure (including the `audit.recorded`
  consumer). There is no dead-letter exchange. Add a per-queue DLX with
  `x-dead-letter-exchange = hrms.events.dlx` + a retry exchange before relying
  on any event for delivery guarantees (e.g. outbound email). Still open.