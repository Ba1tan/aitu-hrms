# Phase 3 — Payroll

**Goal:** ACCOUNTANT / HR_MANAGER can run an entire payroll cycle through
the UI: create period → generate payslips → review → adjust →
approve → mark paid → lock. Employees see their own payslips with PDF.

This is the biggest single-domain UI in the system. Budget time.

## Required reading

1. `docs/API_CONTRACT.md` (Payroll section, 23 endpoints)
2. `services/payroll-service/PAYROLL_SERVICE.md` — calculator details, KZ tax math
3. `docs/PERMISSIONS.md` (`PAYROLL_*`, `PAYSLIP_*` codes)
4. The root README.md "Kazakhstan Payroll 2026" formula box
5. `bruno/05-Payroll/*.bru` — every Bruno request is a working example of
   how to call the API. The chained ID handling there shows what
   periodId/payslipId flow looks like.

## Tasks

### 1. Periods list (`client/pages/PayrollPeriods.tsx` — rewrite from `Payroll.tsx`)

- Table: period name (Май 2026), year, month, status pill
  (DRAFT/PROCESSING/COMPLETED/APPROVED/PAID/LOCKED), employeeCount,
  totalGross, totalNet, createdBy, actions.
- Status colors per `client/lib/format.ts:statusColor`.
- "Create period" button (`PAYROLL_PROCESS`) opens modal: year/month/workingDays.
- Row click → period detail.

### 2. Period detail (`client/pages/PayrollPeriodDetail.tsx` — new)

Top section:
- Header with period name + status + totals
- Action buttons row, each gated and only visible when state allows:
  - **Generate payslips** (`PAYROLL_PROCESS`, only DRAFT)
  - **Approve** (`PAYROLL_APPROVE`, only COMPLETED)
  - **Mark paid** (`PAYROLL_PAY`, only APPROVED)
  - **Lock** (`SUPER_ADMIN`, only PAID)

Body: payslip table (Task 3 below).

### 3. Payslip table (component inside period detail)

- Columns: employee, gross, earned, allowances, deductions, IPN, OPV, ВОСМС, net, status
- Status filter dropdown (DRAFT/APPROVED/PAID)
- Search on employee name/number — debounced, server-side via
  `/v1/payroll/periods/{id}/payslips?search=`
- Row click → payslip detail side-panel (Task 4)

### 4. Payslip detail (side-panel or full page)

Two variants:

**Admin variant** (`PAYROLL_READ_ALL`):
- Full tax breakdown table: gross, earned, OPV (10%), ВОСМС (2%),
  deduction (30 МРП), taxable, IPN (10% or 20%), employer SO/SN/ОПВР
- Allowances + deductions list with edit buttons (`PAYSLIP_ADJUST`)
- "Recalculate" button → `POST /v1/payroll/payslips/{id}/recalculate`
- PDF download button

**Employee variant** (`PAYSLIP_VIEW_OWN`, own payslip only):
- Same tax breakdown but read-only
- PDF download
- Used from `/my-payslips` route

### 5. Adjustments side-panel

- `GET /v1/payroll/additions?periodId=&employeeId=` for current
- Add row form: type (ADDITION/DEDUCTION), category, amount, isTaxable, description
- After save: recalculate payslip and show new net delta highlighted

### 6. Generate flow

When user clicks **Generate payslips**:

1. POST `/v1/payroll/periods/{id}/generate` with `{ employeeIds: [] }` (all active)
2. Response is sync for small companies, async for >50 employees
3. If async: poll `/v1/payroll/jobs/{jobId}/status` every 2s. Show progress bar
4. On complete: refresh the payslips table

### 7. Employee self-service: my payslips

`client/pages/MyPayslips.tsx` — replace Payroll.tsx for EMPLOYEE-role users.
List + click for detail (employee variant) + PDF download.

Route this conditionally:
```tsx
{user.role === "EMPLOYEE"
  ? <Route path="/payroll" element={<MyPayslips />} />
  : <Route path="/payroll" element={<PayrollPeriods />} />}
```

(Or always-mount `/payroll` to PayrollPeriods and add `/my-payslips` for
employees — your call. Simpler is unified `/payroll` that branches.)

### 8. YTD viewer (`client/pages/PayrollYtd.tsx`)

`GET /v1/payroll/ytd/employee/{id}?year=`. Cumulative totals card grid:
total gross, total net, total OPV, total IPN, total ВОСМС.

## Component checklist

Same set as Phase 1 + `progress` (for batch job), `chart` (for YTD viz —
Recharts already in deps).

## Definition of done

- [ ] Full Bruno flow (01 → 11) reproducible through the UI
- [ ] Admin can edit a payslip's allowances, see the recalculated net
- [ ] PDF download triggers a real file (verify `Content-Disposition`)
- [ ] Employee view shows only own payslips
- [ ] Once `Approve` is clicked, the action buttons swap correctly
- [ ] `typecheck` + `build:client` green

## Things to avoid

- **Don't compute payroll math on the frontend.** Display only. The
  backend's `KazakhstanPayrollCalculator` is the source of truth.
- **Don't show employer-side taxes (SO, SN, ОПВР) to employees** — even
  the employee variant of the detail page hides them.
- **BigDecimal-as-string everywhere.** Backend sends `"500000.00"`, not
  `500000`. Parse with `parseFloat` only at render time via `formatKZT()`.
- **Status transitions are server-side** — don't try to enable/disable
  buttons based on a state machine you wrote in JS. Trust the period's
  `status` field and the backend's 400 on illegal transitions.
- **Spring Batch may launch async.** Build the polling code first — it's
  easy to forget and ship a UI that just hangs on bigger companies.

## Estimated effort

7–10 days. Adjustments side-panel and the generate-with-polling flow are
the two main time sinks.