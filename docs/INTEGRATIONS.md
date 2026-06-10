# INTEGRATIONS — External Systems

External-system contracts that integration-hub speaks. **This doc is
intentionally a skeleton** — Nursultan / Askar must fill in the concrete
bits before integration-hub can ship.

---

## 1. 1C:Enterprise — Payroll sync

### 1.1 Scope

After payroll period approval, push the period totals + per-employee
payslip data into the customer's 1C:Enterprise instance for accounting
entries and Form 200.00 quarterly tax declaration.

### 1.2 Direction

HRMS → 1C only (write). Read-back is out of scope for v1.

### 1.3 Trigger

`PayrollPeriodApprovedEvent` is consumed by integration-hub, which:
1. Reads the period + all payslips from payroll-service via Feign.
2. Maps to the 1C JSON payload (§1.5).
3. POSTs to the 1C HTTP service.
4. Persists the result + 1C document IDs in `hrms_integration.sync_jobs`.
5. On failure, retry per circuit breaker policy in §1.7.

### 1.4 Endpoint

**TODO** — fill in once we have a 1C dev instance to point at.

| Field | Value |
|---|---|
| Base URL | `${ONE_C_BASE_URL}` (per-customer) |
| Path | `/hs/hrms/payroll/sync` (placeholder — confirm with 1C side) |
| Method | `POST` |
| Auth | HTTP Basic (`${ONE_C_USERNAME}` / `${ONE_C_PASSWORD}`) |
| Content-Type | `application/json; charset=utf-8` |
| Idempotency-Key | `payroll-sync-{periodId}` (header) |
| Timeout | 30s connect+read |

### 1.5 Request payload — DRAFT

```json
{
  "period": {
    "id":     "UUID",
    "year":   2026,
    "month":  3,
    "approvedAt": "2026-04-05T10:00:00+05:00",
    "approvedBy": "UUID (HRMS user id)"
  },
  "totals": {
    "employeeCount":  142,
    "totalGross":     "67500000.00",
    "totalNet":       "55890000.00",
    "totalOpv":       "6750000.00",
    "totalVosms":     "1350000.00",
    "totalIpn":       "5510000.00",
    "totalSo":        "3037500.00",
    "totalSn":        "4050000.00",
    "totalOpvr":      "2362500.00"
  },
  "payslips": [
    {
      "employeeId":   "UUID",
      "iin":          "123456789012",   // KZ tax ID
      "fullName":     "Иванов Иван Иванович",
      "gross":        "500000.00",
      "earned":       "500000.00",
      "opv":          "50000.00",
      "vosms":        "10000.00",
      "ipn":          "33000.00",
      "net":          "407000.00",
      "soEmployer":   "22500.00",
      "snEmployer":   "30000.00",
      "opvrEmployer": "17500.00"
    }
  ]
}
```

> Field naming and shape are placeholders. The 1C developer on the customer
> side must confirm and we update this doc + the DTO before going live.

### 1.6 Expected response — DRAFT

```json
{
  "success": true,
  "syncedAt": "2026-04-05T10:00:01+05:00",
  "documents": [
    { "employeeId": "UUID", "onecDocumentId": "string" }
  ],
  "errors": []
}
```

On HTTP non-2xx or `success: false`, retry per §1.7. On final failure,
mark `sync_jobs` row `FAILED` and emit `integration.1c.sync-failed`
(reserved event — see `docs/EVENTS.md`).

### 1.7 Resilience

- Circuit breaker: Resilience4j, fail rate threshold 50%, slow call
  threshold 10s, wait 60s before half-open probe.
- Retry: 3 attempts, exponential backoff (2s, 4s, 8s), only on 5xx /
  IO exception. **Do not retry on 4xx** — those are likely contract bugs.
- All attempts logged into `hrms_integration.sync_jobs.attempts` array.

### 1.8 Open items

- [ ] 1C dev instance URL + credentials.
- [ ] Confirm payload shape with 1C-side developer.
- [ ] Confirm error response shape from 1C.
- [ ] Idempotency: how does 1C deduplicate? By `periodId` from header, or
      by recomputing a hash?
- [ ] Per-customer config — multiple customers will each have their own
      `ONE_C_BASE_URL`. Promote to per-tenant config table or stay
      single-tenant?

---

## 2. Bank files

### 2.1 Scope

Generate a salary-payment **register** for the customer's bank. This is a
file the accountant downloads and uploads to the bank's business portal
("зарплатный проект" → "загрузить реестр") — there is **no live bank API**.
The bank then runs the interbank transfer itself (Halyk e.g. as MT-102).

Each register row is one beneficiary: **IBAN, ИИН, ФИО, сумма (net), КНП, КБе,
назначение**. The employee IBAN comes from `employees.bank_account`
(entered on the employee form, persisted by employee-service since V3); the
net amount from the period's payslips; company BIN / КНП / КБе / purpose from
company settings.

### 2.2 Trigger

User-initiated: `GET /v1/integration/bank-file/{periodId}` streams the
register as a binary download (`INTEGRATION_MANAGE` or `SYSTEM_SETTINGS`).
No event side-effect. Employees with no IBAN on file are still listed (blank
account) and logged as a warning — the accountant reviews before upload.

### 2.3 Supported formats (implemented)

Generators live in `services/integration-hub/.../service/bank/`. All produce
the same canonical KZ register columns; they differ only in delimiter. Format
key is the `integration.bank_default_format` setting.

| Bank | Format key | Delimiter | Generator |
|---|---|---|---|
| Kaspi | `KASPI_TSV` | tab | `KaspiTsvGenerator` |
| Halyk | `HALYK_CSV` | comma | `HalykCsvGenerator` |
| Jusan | `JUSAN_CSV` | semicolon | `JusanCsvGenerator` |

КНП / КБе / purpose are configurable: `integration.bank_knp` (default `010`),
`integration.bank_kbe` (default `19` — resident individuals),
`integration.bank_payment_purpose` (default `Заработная плата за MM.YYYY`).

> **Still needs per-bank validation.** The exact column order / headers /
> encoding differ per bank's current template. These generators emit UTF-8
> (BOM, CRLF); some banks require **CP1251**. Confirm each bank's spec against
> their corporate-banking template before go-live, and verify the КНП code
> against the current НБРК classifier (`010` was the historical salary code;
> some sources now map it to ОПВ).

### 2.4 File-naming convention (proposed)

```
{bank}-salary-{periodYear}-{periodMonth:02}-{generatedAt:yyyymmddhhmmss}.{ext}
```

Example: `kaspi-salary-2026-03-20260405093000.tsv`.

### 2.5 Storage

Today the register is **streamed straight to the download** — it is not
persisted server-side. Writing copies to `${BANK_FILE_DIR:/data/hrms/bank-files}`
and indexing them in `hrms_integration.bank_files` for a 1-year retention
(see `docs/COMPLIANCE.md` §3) is still open — see §2.6.

### 2.6 Open items

- [x] IBAN-based register with configurable КНП/КБе/purpose (V3 employee bank
      account + integration-hub generators).
- [ ] Confirm which banks customer #1 uses and validate each register against
      that bank's portal template (column order, headers).
- [ ] Encoding: generators emit UTF-8; add CP1251 output for banks that need it.
- [ ] Persist + index generated files for retention (§2.5).
- [ ] Decide signing: some banks require a digital signature (NCALayer/EDS).
      That's a separate workflow with the user's hardware key.

---

## 3. Government / tax integrations

### 3.1 Form 200.00 (quarterly tax declaration)

Generated by reporting-service (`GET /v1/reports/form200`) as XLSX matching
the State Revenue Committee template. Filing happens **out of band** — the
customer's accountant uploads the file via the SRC's e-government portal.
Out of scope for HRMS to file directly.

### 3.2 SGD / pension fund (ОПВ remittance)

Same as Form 200.00 — HRMS calculates and outputs; the customer files.

### 3.3 Future: direct e-filing via "Кабинет налогоплательщика" API

Reserved. Likely in 2027+ once the SRC opens up programmatic submission.

---

## 4. Email — outbound

| Provider | Used by |
|---|---|
| SMTP (configurable per env) | notification-service |

Default: Gmail SMTP for dev. Production should use a KZ-hosted relay
(e.g., Cloud-KZ Mail) — see `docs/COMPLIANCE.md` §7 on cross-border.

---

## 5. Push notifications — Firebase Cloud Messaging

Optional. notification-service supports FCM if `FCM_CREDENTIALS_JSON` is
set. Read the cross-border note in `docs/COMPLIANCE.md` §7 before
enabling for KZ-resident users.

---

## 6. SMS — out of scope for v1

Schema column `notifications.channel` accepts `SMS` but no provider is
wired. Pick one of:
- Mobizon.kz (KZ SMS aggregator)
- Twilio (international, more expensive, may have cross-border issues)

Do not enable until §3 of `docs/COMPLIANCE.md` is met for the chosen
provider.