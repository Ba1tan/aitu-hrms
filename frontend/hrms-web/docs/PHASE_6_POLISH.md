# Phase 6 — Polish & cross-cutting

**Goal:** Ship-quality. Before the first customer touches it, the frontend
needs i18n, a theme system, a real test plan, accessibility baseline,
mobile responsiveness, error boundaries, and code-splitting. This phase
isn't a single feature — it's the work that turns "working demo" into
"shippable product."

Do **after** Phases 1–5 are at least roughed in. Some of these items can
slot in earlier as you encounter the need.

## Required reading

1. `docs/COMPLIANCE.md` (repo root) — KZ PDPL constraints on display data
2. `docs/OPERATIONS.md` (repo root) — what's already wired (Sentry? Loki?)
3. `frontend/hrms-web/AGENTS.md` — money/i18n conventions

## Tasks

### 1. i18n (`react-i18next`)

KZ target → Russian primary, Kazakh required, English nice-to-have.

- Install: `react-i18next` + `i18next` + `i18next-browser-languagedetector`
- `client/i18n/index.ts` — bootstrap.
- `client/i18n/locales/{ru,kk,en}.json` — namespace per page:
  `common`, `employees`, `payroll`, `leave`, `attendance`, `auth`,
  `dashboard`, `reports`, `settings`.
- Replace every hard-coded user-facing string with `t("namespace.key")`.
  Backend-emitted values (employee names, statuses) stay as-is.
- Locale switcher in the topbar (dropdown), persists to localStorage.
- Status enum translations live in `common.statuses.<ENUM_VALUE>` —
  centralizes the UI labels for ACTIVE / ON_LEAVE / TERMINATED / etc.

Russian is fully fleshed; Kazakh + English can be 80% with English
fallback for missing keys.

### 2. Theme system

- Theme tokens: `client/lib/theme.ts` — design tokens for color/spacing/radius.
- Light + dark variants. Tailwind's `class="dark"` strategy on `<html>`.
- Toggle in topbar; persist to localStorage; respect `prefers-color-scheme`
  on first load.
- Audit all pages for hardcoded hex (`DashboardLayout.tsx` is the worst
  offender — `#3B82F6`, `#00C896`). Move to CSS variables.
- shadcn's components already use CSS variables — extend
  `client/global.css` with the dark theme block.

### 3. Test plan

**Unit (Vitest, already in deps):**
- Format helpers (`formatKZT`, `maskIin`, date formatters).
- Auth helpers (`decodeJwt`, `TokenService.isAuthenticated`).
- Permission gate component (`<RequirePermission>` with each prop combo).

**Component (Vitest + @testing-library/react):**
- One smoke test per page: renders without throwing.
- Permission-gated buttons: render for SUPER_ADMIN, hide for EMPLOYEE.

**Integration (Playwright, new):**
- Install: `@playwright/test`.
- `tests/e2e/auth.spec.ts` — login, redirect to /dashboard, logout.
- `tests/e2e/employee-flow.spec.ts` — create department → create employee
  → view detail.
- `tests/e2e/payroll-flow.spec.ts` — create period → generate → approve.
- Run against staging (`192.168.100.53:8091`) in CI, nightly.

**Visual regression (optional, Playwright):**
- Screenshot key pages, diff per PR. Catches CSS regressions.

### 4. Accessibility baseline

- Run `axe-core` via `@axe-core/playwright` in E2E tests. Fail on serious+.
- Manual audit checklist:
  - All form inputs have labels (`<label for>` or `aria-label`).
  - Focus rings visible everywhere (no `outline: none` without replacement).
  - Modal dialogs trap focus + restore on close (Radix already does this).
  - All interactive elements reachable by Tab.
  - Color contrast ≥ 4.5:1 for text. Use a contrast checker.
  - Status-color pills also carry text or an icon (color alone fails a11y).
- Document the result in `frontend/hrms-web/docs/A11Y_AUDIT.md`.

### 5. Mobile responsiveness

- Audit all pages at 375px width (iPhone SE). Sidebar collapses to a
  hamburger; tables become card lists or get horizontal scroll.
- Use Tailwind breakpoints (`sm:` `md:` `lg:`). DashboardLayout's
  current `width: 240px` sidebar must become a `Sheet` (Radix already
  installed) on mobile.
- Forms: full-width fields on mobile; multi-column grid only at `md+`.

### 6. Error boundaries

- `client/components/ErrorBoundary.tsx` — wraps each route.
- Catches render errors; shows a friendly "Something went wrong — reload
  this page" card with a "Report an issue" button (mailto: support).
- In dev: render the actual error stack.
- Don't catch async errors — TanStack Query has its own error state UI.

### 7. Performance

- **Code-split routes:** every `import Page from "./pages/X"` becomes
  `const Page = lazy(() => import("./pages/X"))`, wrapped in
  `<Suspense fallback={<RouteSpinner />}>` at the route level.
- Current bundle is 609 kB (gzip 192 kB) — fine for now, but the heavy
  pages (org chart with `reactflow`, charts with Recharts) should be
  split out so the auth + dashboard load is < 100 kB gzip.
- Run `pnpm build:client -- --mode analyze` with `rollup-plugin-visualizer`
  to confirm what's in the main bundle.
- Lazy-load fonts (only the weights you actually use).

### 8. Network / offline UX

- Detect offline state (`navigator.onLine` + `online`/`offline` events) —
  show a toast.
- Retry button on any failed query (TanStack Query exposes `refetch`).
- The axios interceptor already handles 401 → refresh. For 5xx, show a
  generic "Service temporarily unavailable" toast.

### 9. Bundle hardening

- Audit dependencies: `pnpm audit` in CI.
- Pin React + Vite + Tailwind majors; allow patch upgrades automatically.
- Remove the broken `vite.config.server.ts` (Express server isn't used in
  prod — nginx serves the SPA). Update `package.json` scripts to drop
  `build:server`. See the Phase 0 "things to avoid" — server build error
  is currently masked but should be cleaned up.

### 10. Observability (when backend has it)

- `Sentry.init(...)` once SENTRY_DSN env var is in place.
- Wrap React with `<Sentry.ErrorBoundary>`.
- Add `release` tag = `process.env.VITE_RELEASE_SHA`.
- Tag user with email + role after login.

## Component checklist

- `sheet` (mobile sidebar)
- `command` (locale switcher, search)
- `dropdown-menu`, `tooltip`, `dialog` (all already present)

## Definition of done

- [ ] Three locales (`ru`, `kk`, `en`) ship; Russian is 100% coverage;
  Kazakh + English ≥ 80% with English fallback.
- [ ] Light + dark theme; toggle persists.
- [ ] All form labels associated; focus rings visible; axe-core reports 0
  serious+ violations on every route.
- [ ] All routes render correctly at 375px width.
- [ ] Error boundaries catch render errors per route; show recovery card.
- [ ] Initial bundle (dashboard + auth) < 150 kB gzip.
- [ ] Lazy-loaded routes verified in network tab.
- [ ] Unit + component tests pass via `pnpm test`.
- [ ] At least 3 Playwright E2E tests pass against staging.
- [ ] `pnpm build:client` green; server build removed from `build` script.

## Things to avoid

- **Don't add a state manager (Redux / Zustand) just for theme + locale.**
  CSS variables + i18next handle it.
- **Don't translate backend-emitted enums on the frontend manually** —
  the backend already returns localized strings for fields like
  attendance status when needed. Translate via the lookup table approach
  (`common.statuses.ACTIVE = "Активен"`).
- **Don't ship a "Beta" / "Early access" banner** unless the customer
  asked for it. Quietly working is the goal.

## Estimated effort

5–8 days, can be parallelized across two people (one on i18n + theme,
the other on tests + a11y + perf).