# Frontend Phase Pipeline

A six-phase plan to take the HRMS frontend from a thin scaffold to a fully
working multi-role UI. Each phase has its own MD file; pick one, brief a
Claude session with it, and the session should be self-contained.

| Phase | Status | Doc | Outcome |
|-------|--------|-----|---------|
| 0 — Foundation | ✅ done | [PHASE_0_FOUNDATION.md](PHASE_0_FOUNDATION.md) | Real auth + permission gates + refresh-retry |
| 1 — Core HR | ☐ | [PHASE_1_CORE_HR.md](PHASE_1_CORE_HR.md) | Depts/positions/employees CRUD + detail page; **1B: admin (users, audit, roles)** |
| 2 — Attendance & Leave | ☐ | [PHASE_2_ATTENDANCE_LEAVE.md](PHASE_2_ATTENDANCE_LEAVE.md) | Check-in, approvals, calendars; **2B: face-kiosk + fraud queue** |
| 3 — Payroll | ☐ | [PHASE_3_PAYROLL.md](PHASE_3_PAYROLL.md) | Periods, payslips, adjustments, AI flags |
| 4 — Self-service & Setup wizard | ☐ | [PHASE_4_SELF_SERVICE.md](PHASE_4_SELF_SERVICE.md) | Personal dashboard, /setup, profile, **full notifications inbox + preferences** |
| 5 — Reports & integrations | ☐ | [PHASE_5_REPORTS.md](PHASE_5_REPORTS.md) | Report picker, charts, 1C history; **5B: AI insights, attrition, forecast** |
| 6 — Polish & cross-cutting | ☐ | [PHASE_6_POLISH.md](PHASE_6_POLISH.md) | i18n (ru/kk/en), theme, tests, a11y, mobile, perf |

Phases marked **B** are extensions of the main phase doc — same file,
later sections. They're cleanly separable so you can ship the main phase
first and circle back, or do both together if a doc lines up nicely.

## How each phase doc is structured

Every phase doc has the same shape so a Claude session can pick one up cold:

```
1. Goal — what "done" looks like
2. Required reading — files to ingest BEFORE writing code
3. Tasks — numbered, each with file paths + endpoint refs
4. Component checklist — which shadcn primitives this phase uses
5. Definition of done
6. Things to avoid — gotchas already learned the hard way
```

## Conventions used in every phase

- **API:** import from `shared/api.ts` (axios + interceptors already do auth +
  refresh + envelope unwrap). New per-domain client functions go in the same
  file under their own `<domain>Api` object.
- **State:** TanStack Query for server state. Local component state is fine
  for forms; lift to Zustand only if shared across siblings.
- **Forms:** React Hook Form + Zod. Schemas live near the form unless reused.
- **Money:** backend sends numeric strings — never `JSON.parse` to number.
  Use the `formatKZT()` helper (`client/lib/format.ts` — create in Phase 1
  if it doesn't exist).
- **Dates:** ISO-8601 on the wire; render with `date-fns` and the user locale.
  Russian primary.
- **Permission gating:** wrap mutate buttons/sections in
  `<RequirePermission code="...">`. Permission codes come from
  `docs/PERMISSIONS.md` at the repo root.
- **Loading:** every list/detail page has a skeleton state. Empty states have
  a CTA, not just "No data".
- **Toast:** `import { toast } from "sonner"`. Errors come back as
  `error.response?.data?.message`.

## Where to read first (one-time, all phases benefit)

| Doc | What it tells you |
|-----|-------------------|
| `docs/API_CONTRACT.md` (repo root) | Every endpoint, permission, request/response shape |
| `docs/PERMISSIONS.md` (repo root) | Canonical permission codes + role → permission mapping |
| `frontend/hrms-web/AGENTS.md` | Frontend conventions (this file's parent) |
| `frontend/hrms-web/shared/api.ts` | All API client wiring + DTOs |
| `frontend/hrms-web/shared/auth.ts` | Token storage + JWT decode |
| `frontend/hrms-web/client/providers/AuthProvider.tsx` | Auth state + hasPermission |
| `frontend/hrms-web/client/providers/RequirePermission.tsx` | UI gating pattern |

## How a Claude session should run a phase

Drop this prompt at the start of a session:

> Implement Phase N of the HRMS frontend per
> `frontend/hrms-web/docs/PHASE_N_*.md`. Read the doc first, then ingest
> any files it points at as "required reading". Work through the Tasks
> section sequentially. When done, run `npx pnpm typecheck` and
> `npx pnpm run build:client` to verify, then commit per the phase's
> definition of done.

That's enough — every phase doc is self-contained.