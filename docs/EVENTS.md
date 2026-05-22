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
| `employee.created` | `EmployeeCreatedEvent` | employee-service | user-service (`Q_EMPLOYEE_CREATED`), leave-service (`leave.employee.created`), payroll-service (`payroll.employee.created`), notification-service 🟡 | ✅ |
| `employee.terminated` | `EmployeeTerminatedEvent` | employee-service | user-service 🟡 (deactivate account), payroll-service 🟡 (stop calc), leave-service 🟡 (cancel pending), notification-service 🟡 | ✅ (publisher only — no listeners wired yet) |
| `employee.salary.changed` | `SalaryChangedEvent` | employee-service | payroll-service (`payroll.employee.salary.changed`), notification-service 🟡 | ✅ |
| `attendance.recorded` | `AttendanceRecordedEvent` | attendance-service | reporting-service 🟡 (dashboard cache invalidation), notification-service 🟡 (late-arrival nudge) | ✅ (publisher only) |
| `leave.request.created` | `LeaveRequestCreatedEvent` | leave-service | notification-service 🟡 (notify approver) | ✅ (publisher only) |
| `leave.approved` | `LeaveApprovedEvent` | leave-service | attendance-service (`attendance.leave.approved`) auto-marks ON_LEAVE; notification-service 🟡 (notify employee) | ✅ |
| `leave.rejected` | `LeaveRejectedEvent` | leave-service | notification-service 🟡 (notify employee) | ✅ (publisher only) |
| `payroll.job.started` | `PayrollJobStartedEvent` | payroll-service | reporting-service 🟡 (cache lock), notification-service 🟡 (HR progress notice) | ✅ (publisher only) |
| `payroll.job.completed` | `PayrollJobCompletedEvent` | payroll-service | reporting-service 🟡 (invalidate dashboard cache, pre-gen XLSX), notification-service 🟡 (HR notice) | ✅ (publisher only) |
| `payroll.period.approved` | `PayrollPeriodApprovedEvent` | payroll-service | notification-service 🟡 (per-employee payslip-ready), integration-hub 🟡 (1C sync trigger) | ✅ (publisher only) |
| `user.account.created` | `UserAccountCreatedEvent` | user-service | notification-service 🟡 (welcome email + temp password) | ✅ (publisher only) |
| `user.password.reset-requested` | `PasswordResetRequestedEvent` | user-service | notification-service 🟡 (reset link email) | ✅ (publisher only) |

### Pending events (reserved names — Askar should add when implementing)

| Routing key | When emitted | Producer | Notes |
|---|---|---|---|
| `payslip.adjusted` 🟡 | Per-employee correction inside an approved period | payroll-service | Reuse `PayslipAdjustedEvent` shape — payslipId, employeeId, periodId, correctionAmount |
| `report.generated` 🟡 | Long-running XLSX/PDF finished | reporting-service | Allows notification-service to push "Your report is ready" |
| `integration.1c.synced` 🟡 | 1C sync job finished | integration-hub | Drives an audit log row + dashboard "last sync at X" |
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

---

## 4. Consumer queues currently bound

| Queue | Consumer service | Routing key |
|---|---|---|
| `user.employee.created` | user-service | `employee.created` |
| `payroll.employee.created` | payroll-service | `employee.created` |
| `payroll.employee.salary.changed` | payroll-service | `employee.salary.changed` |
| `leave.employee.created` | leave-service | `employee.created` |
| `attendance.leave.approved` | attendance-service | `leave.approved` |

Pending services should pre-declare their queues in their own `RabbitConfig`
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
- **Several listeners on the consumer side don't exist yet** — most notably
  notification-service has no consumers wired but is documented as the
  consumer for 11 of the 14 events. Treat the 🟡 markers above as Askar's
  TODO list.
- **`employee.terminated`, `attendance.recorded`,
  `leave.request.created`, `leave.rejected`, `payroll.*` events are
  publish-only today** — every reactive workflow downstream is currently a
  no-op. This is acceptable until notification-service ships, but it does
- **No DLQ / retry policy declared** — every queue is plain durable. Once
  notification-service starts sending email, add a per-queue DLX with
  `x-dead-letter-exchange = hrms.events.dlx` and a retry exchange. See
  `docs/OPERATIONS.md` once that doc exists.