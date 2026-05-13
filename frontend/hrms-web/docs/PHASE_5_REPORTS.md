# Phase 5 — Reports & integrations

**Goal:** UI for downloadable reports, executive-level charts on the
dashboard, and the 1C / bank-file integration history.

This phase depends on services that aren't built yet
(reporting-service, integration-hub). Build the UI shells now;
hook up endpoints as they ship.

## Required reading

1. `services/reporting-service/REPORTING_SERVICE.md` — 13 endpoints
2. `services/integration-hub/INTEGRATION_HUB.md` — 1C + bank file
3. `docs/INTEGRATIONS.md` — what we know (and don't) about 1C / bank formats
4. `docs/PERMISSIONS.md` — `REPORT_*`, `INTEGRATION_MANAGE`

## Tasks

### Reports picker (`client/pages/Reports.tsx` — rewrite)

Card grid, one per report type. Each card: icon, title, description,
"Generate" button. Click opens a parameter modal (period selector / year /
department picker as appropriate), then triggers the download.

The 13 reports per `REPORTING_SERVICE.md`:

| Card | Endpoint | Permission |
|---|---|---|
| Payroll summary (XLSX) | `/v1/reports/payroll-summary?periodId=` | `REPORT_PAYROLL` |
| Payroll summary (PDF) | `/v1/reports/payroll-summary/pdf` | `REPORT_PAYROLL` |
| Form 200.00 | `/v1/reports/form200?year=&quarter=` | `REPORT_PAYROLL` |
| Salary breakdown | `/v1/reports/salary-breakdown?departmentId=` | `REPORT_PAYROLL` |
| Attendance monthly | `/v1/reports/attendance-monthly?year=&month=` | `REPORT_ATTENDANCE` |
| Attendance summary | `/v1/reports/attendance-summary?year=&month=` | `REPORT_ATTENDANCE` |
| Leave balances | `/v1/reports/leave-balances?year=` | `REPORT_LEAVE` |
| Employee directory | `/v1/reports/employee-directory` | `REPORT_HR` |
| Turnover | `/v1/reports/turnover?year=` | `REPORT_HR` |
| Headcount | `/v1/reports/headcount?from=&to=` | `REPORT_HR` |
| Executive summary | `/v1/reports/executive-summary?year=&month=` | `REPORT_EXECUTIVE` |
| AI insights | `/v1/reports/ai-insights` | `AI_DASHBOARD` |

Download pattern (already half-wired in `shared/api.ts:reportsApi`):

```ts
const resp = await apiClient.get(url, { params, responseType: "blob" });
const blob = new Blob([resp.data], { type: resp.headers["content-type"] });
const a = document.createElement("a");
a.href = URL.createObjectURL(blob);
a.download = resp.headers["content-disposition"]?.match(/filename="?(.+?)"?$/)?.[1] || "report";
a.click();
URL.revokeObjectURL(a.href);
```

Gate each card with `<RequirePermission code="...">`.

### Executive dashboard (`client/pages/ExecutiveDashboard.tsx` — new)

DIRECTOR-only landing. Recharts-powered:

- Headcount trend (last 12 months) — line chart
- Payroll cost trend — bar chart, gross vs net per month
- Department breakdown — pie chart (employee count per dept)
- AI anomaly count — sparkline
- Attrition risk — top-5 employees ranked by `ai-ml-service`'s attrition score

Until reporting-service ships, aggregate client-side from the existing
endpoints. Will be slow with many employees — acceptable as a stopgap.

### Integration history (`client/pages/IntegrationHistory.tsx`)

`/v1/integration/sync/history` — table of 1C sync jobs:
status, periodId → period name lookup, target, triggeredAt, retryCount,
errorMessage, "Retry" button (`POST /v1/integration/retry/{jobId}`).

### Bank file download

In PayrollPeriodDetail (Phase 3 page), add a "Download bank file" action
once `PAID`. Calls `GET /v1/integration/bank-file/{periodId}` with blob
response. Permission: `INTEGRATION_MANAGE`.

### Company settings page (`client/pages/Settings.tsx`)

`SYSTEM_SETTINGS` only. Form UI on top of `/v1/settings` key-value:
group keys by category (company.*, attendance.*, payroll.*, leave.*, integration.*).
Each setting renders as the right input type.

Same plumbing as the setup wizard, just always-on for SUPER_ADMIN.

## Component checklist

Phase 1's set + `chart` (Recharts is already in deps), `date-range-picker`
(custom over `Calendar`), `progress`.

## Definition of done

- [ ] All 13 report cards render with correct gating
- [ ] At least one report (e.g. employee directory) actually downloads
  end-to-end once reporting-service ships
- [ ] Executive dashboard renders without errors when at least one period exists
- [ ] Integration history table renders empty state cleanly
- [ ] Settings page lets SUPER_ADMIN edit a setting and see it persist
- [ ] `typecheck` + `build:client` green

## Things to avoid

- **Don't aggregate executive metrics on the frontend in production.**
  This is a temporary stopgap. Once reporting-service ships with
  materialized views, switch the page over.
- **Don't show the 1C URL/password fields on the settings page in plain text.**
  Use a `<input type="password">` even for the URL, and show `********` for
  values whose key ends in `_password` / `_token` / `_secret` (the backend
  already returns those masked).
- **Don't try to write the Form 200.00 XLSX layout from scratch on the
  frontend.** That's reporting-service's job. The frontend just calls the
  endpoint and saves the blob.

## Estimated effort

4–5 days once reporting-service and integration-hub exist. Faster if some
shells are built ahead of those services as placeholders.

---

## Phase 5B — AI features (extension)

Until ai-ml-service ships these endpoints return 502; build the shells
now with empty/placeholder states.

### AI insights landing (`client/pages/ai/Insights.tsx`)

`AI_DASHBOARD` permission. Single page that surfaces the four AI signals:

1. **Recent anomalies** (last 30 days)
   - Source: `/v1/ai/payroll/recent-anomalies` (TBD; or cross-reference
     PayrollAnomalyDetectedEvent rows from a future reporting endpoint)
   - Card list: payslip, employee, anomalyScore, flags, "Review" → opens
     PayrollPeriodDetail with that payslip pre-selected.

2. **Attrition risk top-N** (link to drill-down)
   - Source: `/v1/ai/attrition/risk`
   - Sparkline + top-5 employees by risk score with action: "View profile"

3. **Payroll cost forecast** (next 3 months)
   - Source: `/v1/ai/payroll/forecast?months=3`
   - Bar chart, predicted gross/net per month with 80% confidence interval

4. **Recent fraud attempts** (last 7 days)
   - Source: derive from biometric_attempts (Phase 2B)
   - Map view by deviceId if location columns are populated

### Attrition risk page (`client/pages/ai/Attrition.tsx`)

- Table of all employees with risk score, level (LOW / MEDIUM / HIGH),
  top 3 contributing factors (tenure, salary ratio, overtime, leave usage…).
- Filters: department, risk level, role.
- Row click → detail panel with: full feature breakdown (radar chart),
  retention recommendations from the AI service.
- Endpoint: `GET /v1/ai/attrition/risk?departmentId=`.

### Payroll forecast viewer (`client/pages/ai/Forecast.tsx`)

- Chart: predicted payroll cost per month, next 12 months.
- Controls: months horizon (3/6/12), confidence interval toggle.
- Below chart: assumption table (current headcount, avg salary, growth
  rate from past quarters).
- Endpoint: `GET /v1/ai/payroll/forecast?months=`.

### Sidebar nav

Add "AI Insights" group with the three pages above. Wrap in
`<RequirePermission code="AI_DASHBOARD">`.

### Definition of done (Phase 5B)

- [ ] All three AI pages render with empty states when service is 502
- [ ] Each page is permission-gated (only DIRECTOR / SUPER_ADMIN / HR_MANAGER see them)
- [ ] Charts use Recharts (already in deps); no new chart lib pulled in
- [ ] Forecast chart with horizon controls works without backend (mock data)