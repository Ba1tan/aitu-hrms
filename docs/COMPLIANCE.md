# COMPLIANCE — Kazakhstan PDPL & Data Retention

HRMS targets KZ SMEs and is subject to the **Law of the Republic of Kazakhstan
"On personal data and its protection"** (Закон РК от 21.05.2013 № 94-V,
known as PDPL or "ЗоПД"). This document is the operational stance — it is
**not** legal advice and must be reviewed by counsel before launch with a
real customer.

---

## 1. What counts as personal data here

Per PDPL Art. 1, "personal data" is any information about an identified or
identifiable natural person. In this system that includes:

| Category | Stored where | Sensitivity |
|---|---|---|
| Full name, IIN, contact details, photo | `hrms_employee.employees` | High (IIN is regulated) |
| Salary, bank details, payslips | `hrms_employee`, `hrms_payroll` | High |
| Biometric (face photos + embeddings) | `/data/hrms/uploads/employees/{id}/biometric/`, ai-ml-service embedding store | **Special category** — written consent required |
| Attendance check-ins (with photo on face check-in) | `hrms_attendance.attendance_records`, `biometric_attempts` | Medium |
| Leave reasons (may include medical) | `hrms_leave.leave_requests.reason` | High |
| Audit log (who did what) | `hrms_user.audit_logs` | Internal |

---

## 2. Lawful basis

| Purpose | Basis (PDPL Art. 8) |
|---|---|
| Employment lifecycle, payroll calculation | Performance of employment contract |
| KZ tax / state pension reporting | Compliance with legal obligation |
| Biometric check-in | **Explicit written consent** of the employee — required before enrollment |
| AI fraud / anomaly flagging | Legitimate interest (operator's right) — must be documented |
| Marketing / non-employment use | Out of scope. Do not add. |

---

## 3. Retention policy

PDPL doesn't set a single retention period — it says "for the period
necessary for the purpose." The schedule below is operational policy.

| Data | Retention | Trigger | Method |
|---|---|---|---|
| Employee profile | Active employment + 5 years | Termination + 5y | Soft delete on termination; hard delete after 5y via batch job |
| Biometric photos + embeddings | Active employment only | Termination | **Hard delete immediately** on termination — legal minimum |
| Payslips (PDF + DB row) | 5 years | Period close | KZ Tax Code Art. 48 — keep 5y from period |
| Audit logs | 3 years | Row creation | Trim batch monthly |
| Attendance records | 3 years | Workday | Trim batch monthly |
| Leave requests | Active employment + 1 year | Decision date | Trim batch monthly |
| Notifications (sent) | 6 months | Sent date | Trim batch monthly |
| Bank file artifacts | 1 year | Generation | File system trim |
| 1C sync log | 1 year | Sync date | Trim batch monthly |

Implementation: a `cleanup` Spring Batch job per service, scheduled monthly
(reporting-service is a sensible host for the orchestration). Each job
soft-deletes rows past retention, then hard-deletes soft-deleted rows past
the grace period.

---

## 4. Right to be forgotten (Art. 12)

A subject can request deletion. Unless there is a legal-obligation reason
to keep the record (most commonly tax law for payroll), the operator must
delete within **30 calendar days**.

### Workflow when an ex-employee requests deletion

1. SUPER_ADMIN opens `/v1/employees/{id}` and triggers "GDPR delete".
2. employee-service:
   - Drops biometric photos + embeddings (call ai-ml-service to drop the
     embedding row).
   - Anonymizes profile: name → `Удалённый сотрудник`, email → `null`,
     phone → `null`, IIN → `null`. Keep `id` for FK integrity.
3. Publish `EmployeePurgedEvent` (new event — TBD; add to `docs/EVENTS.md`).
4. Each service consuming the event anonymizes its own rows:
   - attendance: keeps records but null-ifies any free-text fields.
   - leave: keeps records, null-ifies `reason`.
   - payroll: **does not delete payslips** — required by tax law for 5y.
     Anonymize the cached employee snapshot only.
   - notification: hard-delete all rows (no legal retention obligation).

---

## 5. Biometric consent

Required artifact: a signed **biometric consent form** in Russian + Kazakh,
stored in `employee_documents` with type `BIOMETRIC_CONSENT`. Enrollment
endpoint (`POST /v1/employees/{id}/biometric/enroll`) MUST refuse if no
active consent document exists. This check is not implemented today — add
it before signing the first customer.

---

## 6. Data minimization

- IIN is required by KZ tax law. Store, but never display in full in lists
  or exports — last 4 only on screen.
- Salary detail is gated by `EMPLOYEE_SALARY_VIEW` and `PAYROLL_VIEW` —
  do not surface salary in employee directory exports without explicit
  permission.
- Logs MUST NOT log full IIN, full name + DOB + address, salary numeric,
  or biometric content. Use `LogSanitizer` (TBD utility) before any
  `log.info`.

---

## 7. Cross-border transfer

PDPL Art. 16 — personal data may not be transferred to a country without
"adequate protection" without explicit subject consent or specific
exceptions. Practical implications:

- Hosting must be in Kazakhstan (or a jurisdiction with PDPL adequacy).
  Today's deploy at `hrms.nursnerv.uk` is fine if hosted in KZ — verify
  with the hosting provider.
- AI inference happens in `ai-ml-service`, which runs on the same host —
  no transfer.
- Email (SMTP) and push (FCM) are external. SMTP is operator-chosen, can
  be a KZ provider. FCM is Google → falls under "Google Workspace KZ
  data processing addendum" or requires individual consent before push
  enrollment.

---

## 8. Operator registration

Per PDPL, operators of personal data systems must register with the State
Service for Information Security. The customer (the SME, not the HRMS
vendor) is the legal "operator" — but we should document this in the
customer-onboarding pack so they don't miss it.

---

## 9. Audit log requirements

- Every mutation to `employees`, `payslips`, `payroll_periods`, and
  `roles`/`role_permissions` writes to `hrms_user.audit_logs`.
- Audit log is read-only after write (DB-level — no `UPDATE` grant on the
  table to the application user).
- Retained per §3 (3 years).
- Viewable only with `SYSTEM_AUDIT` permission.

Today: only user-service writes audit rows. Cross-service audit is on the
TODO list — see `docs/HRMS_ENTERPRISE_ARCHITECTURE.md` §5.

---

## 10. Open items before customer #1

- [ ] Biometric consent gate in employee-service enrollment endpoint.
- [ ] GDPR-delete endpoint + `EmployeePurgedEvent` wiring.
- [ ] Per-service retention batch job.
- [ ] LogSanitizer utility + adoption review.
- [ ] Cross-border check on FCM (notification-service) before enabling push.
- [ ] Confirm hosting region with `nursnerv.uk` provider.
- [ ] Customer-onboarding doc: operator registration reminder.