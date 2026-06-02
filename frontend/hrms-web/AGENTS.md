# HRMS Web — AGENTS.md

**Owner:** Nurbol Sembayev | **Stack:** React 18 + TypeScript + Vite + TailwindCSS + Radix UI

This is the React frontend for HRMS. It talks to the backend exclusively
through the API gateway at `${VITE_API_BASE}/api/...` (default
`https://hrms.nursnerv.uk/api` in prod, `http://localhost:8080/api` in dev).

## Source of truth

- **Backend API:** `docs/API_CONTRACT.md` at the repo root — every endpoint,
  permission, request/response shape, and frontend display convention.
- **Architecture / event flow:** `docs/HRMS_ENTERPRISE_ARCHITECTURE.md`.
- **Permissions:** `docs/PERMISSIONS.md` — role/permission catalog. Use these
  exact codes when guarding UI elements.

If the contract and a backend `services/{name}/{SERVICE}.md` disagree, the
backend service spec is authoritative — let Nursultan/Askar know.

## Phase pipeline

Implementation is staged across seven phases under `frontend/hrms-web/docs/`:

| Phase | Status | Doc |
|-------|--------|-----|
| 0 — Foundation (auth, refresh, permission gates) | ✅ done | `docs/PHASE_0_FOUNDATION.md` |
| 1 — Core HR + 1B Admin (users, audit, roles) | ☐ | `docs/PHASE_1_CORE_HR.md` |
| 2 — Attendance & Leave | ☐ | `docs/PHASE_2_ATTENDANCE_LEAVE.md` |
| 3 — Payroll | ☐ | `docs/PHASE_3_PAYROLL.md` |
| 4 — Self-service + Setup wizard + full Notifications | ☐ | `docs/PHASE_4_SELF_SERVICE.md` |
| 5 — Reports & integrations | ☐ | `docs/PHASE_5_REPORTS.md` |
| 6 — Polish (i18n, theme, tests, a11y, mobile, perf) | ☐ | `docs/PHASE_6_POLISH.md` |

Each phase doc is self-contained (goal → required reading → tasks →
definition of done). Brief a Claude session with "implement Phase N per
`frontend/hrms-web/docs/PHASE_N_*.md`" and it should be able to run cold.

## Project structure

```
frontend/hrms-web/
├── client/              # React SPA
│   ├── pages/           # Route components
│   ├── components/      # Reusable UI; ui/ is the Radix-based primitive layer
│   ├── lib/             # API client, auth, utilities
│   ├── App.tsx          # Router setup
│   └── global.css       # Tailwind + theme
├── shared/              # Types shared with the express dev shim (not used in prod)
├── public/              # Static assets
└── nginx.conf           # Production reverse proxy config
```

## Conventions

- All API calls go through `client/lib/api.ts` (axios with JWT interceptor +
  refresh-token retry).
- Permission gating: `<RequirePermission code="EMPLOYEE_VIEW_ALL">…</RequirePermission>`.
  Permission codes come from `docs/PERMISSIONS.md` — do not invent new codes.
- Forms: React Hook Form + Zod schemas mirroring backend DTOs.
- Tables: TanStack Table with server-side paging (`?page=&size=`).
- Dates: ISO-8601 on the wire; render with date-fns and the user's locale.
- Money: backend returns numeric strings — never JS `number`. Display with
  `Intl.NumberFormat('ru-KZ', { style: 'currency', currency: 'KZT' })`.
- i18n: Russian primary, Kazakh + English supported. Use `react-i18next` keys,
  not hardcoded strings.

## Dev commands

```bash
pnpm install
pnpm dev          # Vite dev server on :3000 with proxy to gateway
pnpm typecheck
pnpm test         # Vitest
pnpm build        # Production build → dist/
```

## Adding a feature

1. Find the endpoint in `docs/API_CONTRACT.md`.
2. Add the typed call to `client/lib/api/{domain}.ts`.
3. Wrap the page in the appropriate permission gate.
4. Use existing primitives in `components/ui/` before adding new ones.
5. If the backend response shape isn't yet documented in the contract, ask
   the service owner (`services/{name}/{SERVICE}.md` lists the owner).

## First-start setup wizard (`/setup`)

A fresh deploy has no company configuration — only the seeded SUPER_ADMIN
account and KZ holidays exist. The wizard runs once per tenant and writes
into the `company_settings` table via integration-hub. See
`services/integration-hub/INTEGRATION_HUB.md` "First-start configuration" for the
authoritative key list and the `/v1/settings/setup-status` contract.

### Routing logic

After every successful login, the auth bootstrap calls
`GET /v1/settings/setup-status`:

```ts
const { configured } = await api.get('/v1/settings/setup-status');

if (!configured) {
  if (currentUser.role === 'SUPER_ADMIN') {
    navigate('/setup');                 // Run the wizard
  } else {
    navigate('/awaiting-setup');         // "Your administrator must finish setup" lock screen
  }
} else {
  navigate(intendedRoute ?? '/dashboard');
}
```

The `/setup` route MUST be reachable for SUPER_ADMIN even when
`configured=false`; do not put it behind the normal app shell that itself
requires a configured tenant.

### Wizard steps

Each step is its own subroute under `/setup` with Next/Back. State is kept
in a single React Hook Form / Zustand store; every Next button persists
that step's settings via `PUT /v1/settings/{key}` so a partial run can be
resumed (the wizard re-reads `GET /v1/settings` on entry to prefill).

| # | Subroute | Writes to backend |
|---|---|---|
| 1 | `/setup/welcome` | (no writes — explainer + admin profile preview) |
| 2 | `/setup/company` | `company.name`, `company.bin` (validate 12 digits), `company.legal_address`, `company.timezone`, `company.currency`, `company.locale_default`, `company.tax_resident` |
| 3 | `/setup/work-schedule` | `POST /v1/attendance/schedules` (start/end/late threshold/working days) → take returned `id` and `PUT /v1/settings/attendance.work_schedule_default_id` |
| 4 | `/setup/holidays` | KZ holidays are pre-seeded — show as a read-only review with "Add custom" + "Remove" buttons calling `POST /v1/attendance/holidays` and `DELETE /v1/attendance/holidays/{id}` |
| 5 | `/setup/attendance-methods` | `attendance.check_in_methods` (multi-select: `WEB`, `MANUAL`, `MOBILE`) |
| 6 | `/setup/department` | First department — `POST /v1/departments` with `name` + optional manager. Skippable. |
| 7 | `/setup/integrations` (optional) | `integration.1c_base_url`, `integration.1c_username`, `integration.1c_password`, `integration.bank_default_format`. All skippable; "Configure later" button. |
| 8 | `/setup/review` | Read-only summary of every value set. "Finish setup" button → `POST /v1/settings/complete-setup`. On 409 with `missingRequired[]`, jump back to the relevant step. |

### Required-key validation

The Finish button is disabled until every required key from
`services/integration-hub/INTEGRATION_HUB.md` is set. The wizard derives this list
client-side from `GET /v1/settings/setup-status` (re-fetch on entry to
`/setup/review`) — do not hardcode the list in the frontend.

### Locking the wizard

After `setup.completed=true`, navigating to `/setup` shows a "Setup is
already complete" page with a link back to `/dashboard`. The route is not
removed — it stays accessible so SUPER_ADMIN can reuse step 7 (integrations)
later, but step 8 is no-op.

### `/awaiting-setup`

Plain page for non-admin users when `configured=false`:
- Localized "Your administrator must complete first-time setup" message.
- "Sign out" button. Nothing else.
- Auto-refreshes `setup-status` every 30s; redirects to `/dashboard` once
  `configured=true`.
