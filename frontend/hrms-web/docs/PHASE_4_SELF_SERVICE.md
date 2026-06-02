# Phase 4 — Self-service & Setup wizard

**Goal:** Two things — make the EMPLOYEE role's day-to-day pleasant
(personal dashboard, profile editing, notifications), and ship the
`/setup` wizard for fresh tenants.

## Required reading

1. `frontend/hrms-web/AGENTS.md` — has the full setup wizard spec
2. `services/integration-hub/INTEGRATION_HUB.md` "First-start configuration" —
   backend contract: required setting keys, `/v1/settings/setup-status`,
   `/v1/settings/complete-setup`
3. `docs/API_CONTRACT.md` Dashboard section (1 endpoint, role-aware)

## Tasks

### Personal dashboard

`client/pages/Dashboard.tsx` — replace placeholder with role-aware cards.

The endpoint `GET /v1/dashboard/stats` is currently 502 (reporting-service
isn't built). Two paths:

- **Now:** stub the data client-side — assemble from existing endpoints
  (employees count + my payslip + my leave balance + today attendance).
- **Later:** swap to `/v1/dashboard/stats` when reporting-service ships.

Cards per role (cribbed from `services/reporting-service/REPORTING_SERVICE.md`):

| Card | Roles | Source |
|---|---|---|
| Employees | all except EMPLOYEE | `/v1/employees?size=1` (read `totalElements`) |
| Last payroll | SUPER_ADMIN/HR_MANAGER/ACCOUNTANT | `/v1/payroll/periods?size=1` |
| Pending leave | SUPER_ADMIN/HR_MANAGER/MANAGER | `/v1/leave/requests?status=PENDING&size=1` |
| Today attendance | SUPER_ADMIN/HR_MANAGER/MANAGER | `/v1/attendance/records/daily` |
| My last payslip | any with employeeId | `/v1/payroll/my-payslips?size=1` |
| My leave balance | any with employeeId | `/v1/leave/balances?year=<current>` |
| Today (self) | any with employeeId | `/v1/attendance/today` |

Use `<RequirePermission>` to hide cards that don't apply.

### Personal profile page

`client/pages/Profile.tsx` — `GET /v1/auth/me` (already wired in
`shared/api.ts`). Read-only display + an "Edit" button that opens a form
limited to:

- firstName, lastName, middleName, phone — fields the user can self-edit
- Salary, role, employeeId, IIN — **read-only** (visible but disabled)
- Password change button → `POST /v1/auth/change-password` modal

Photo upload: `POST /v1/employees/{id}/photo` if/when that endpoint exists.
Defer if not.

### Notifications module (full)

Build the entire UI now; it tolerates an absent notification-service by
catching 502 and rendering empty states.

**1. Bell + popover (`client/components/NotificationsBell.tsx`)**

In the DashboardLayout topbar. Unread count badge. Click → popover with
the latest 5 notifications + a "See all" link to `/notifications`.

**2. Inbox page (`client/pages/Notifications.tsx`)**

- Table: timestamp, type icon (leave / payroll / employee /
  payslip-ready / etc.), title, channel pill (IN_APP/EMAIL/PUSH/SMS),
  read/unread indicator, actions.
- Toolbar: search, type filter, channel filter, "Unread only" toggle,
  "Mark all read" button.
- Row click → side-panel with full message + deep link to the referenced
  entity (e.g. "Payslip approved" → `/payroll/payslips/{id}`).
- Endpoints (per `services/notification-service/NOTIFICATION_SERVICE.md`):
  - `GET /v1/notifications?unread=true|false&type=&page=&size=`
  - `PUT /v1/notifications/{id}/read`
  - `PUT /v1/notifications/read-all`
  - `DELETE /v1/notifications/{id}` (soft delete)

**3. Preferences page (`client/pages/NotificationsPreferences.tsx`)**

Per-event-type × per-channel matrix:

|                          | In-app | Email | Push | SMS |
|--------------------------|--------|-------|------|-----|
| Leave request submitted  | ☑      | ☐     | ☐    | ☐   |
| Leave approved/rejected  | ☑      | ☑     | ☐    | ☐   |
| Payslip ready            | ☑      | ☑     | ☐    | ☐   |
| Account / password event | ☑      | ☑     | ☐    | ☐   |

Backend contract is in `NOTIFICATION_SERVICE.md`. Endpoint `GET/PUT
/v1/notifications/preferences`. SMS column is disabled if backend reports
SMS provider is `none` (read from `/v1/settings/notification.sms_provider`).

**4. Real-time push (deferred)**

Once notification-service exposes a WebSocket / SSE endpoint
(`/v1/notifications/stream`), wire a hook that opens the connection on
auth + closes on logout, dispatches into the query cache. Until then:
poll inbox on window focus.

### Definition-of-done addition (notifications)

- [ ] Bell renders + popover shows latest 5 (or empty state when 502)
- [ ] `/notifications` lists rows + filters work
- [ ] Mark-read flips read indicator optimistically
- [ ] Preferences matrix saves + persists across reload
- [ ] SMS column disabled when settings say `sms_provider=none`

### `/setup` wizard

Full spec lives in `frontend/hrms-web/AGENTS.md` under "First-start setup wizard".
Use that as the implementation brief.

Route structure (nested routes; each step is its own subroute):

```tsx
<Route path="/setup" element={<SetupShell />}>
  <Route index element={<Navigate to="/setup/welcome" replace />} />
  <Route path="welcome"            element={<StepWelcome />} />
  <Route path="company"            element={<StepCompany />} />
  <Route path="work-schedule"      element={<StepWorkSchedule />} />
  <Route path="holidays"           element={<StepHolidays />} />
  <Route path="attendance-methods" element={<StepAttendanceMethods />} />
  <Route path="department"         element={<StepDepartment />} />
  <Route path="integrations"       element={<StepIntegrations />} />
  <Route path="review"             element={<StepReview />} />
</Route>
```

Routing logic (App.tsx, after login):

```tsx
const { data: setupStatus } = useQuery({
  queryKey: ["setup-status"],
  queryFn: () => apiClient.get("/v1/settings/setup-status").then(r => r.data),
  enabled: isAuthenticated,
});

if (setupStatus && !setupStatus.configured) {
  if (user.role === "SUPER_ADMIN") return <Navigate to="/setup" />;
  return <Navigate to="/awaiting-setup" />;
}
```

Each step's "Next" button persists immediately via
`PUT /v1/settings/{key}` so the wizard is resumable.

Final step → `POST /v1/settings/complete-setup` → confetti animation
(seriously, do this — it's a one-time UX) → redirect to `/dashboard`.

### `/awaiting-setup` lock screen

Non-admin users on a fresh tenant see this. Plain message: "Your
administrator must complete first-time setup." Polls `setup-status`
every 30s and redirects when `configured=true`.

## Component checklist

Phase 1's set + `stepper` (build custom — shadcn doesn't ship one),
`progress`, `confetti` (`canvas-confetti`).

## Definition of done

- [ ] EMPLOYEE-role user lands on a dashboard with 3-4 personal cards
- [ ] SUPER_ADMIN lands on a dashboard with admin cards
- [ ] Profile page allows updating phone, returns 200
- [ ] Password change flow works end-to-end (verify in user-service audit log)
- [ ] Notifications bell renders empty state without throwing
- [ ] Fresh tenant (drop `setup.completed` from `company_settings`, log in
  as admin) → routed to `/setup`, can walk through 8 steps, lands on dashboard
- [ ] Non-admin on fresh tenant → routed to `/awaiting-setup`
- [ ] `typecheck` + `build:client` green

## Things to avoid

- **Don't bypass the setup wizard for SUPER_ADMIN.** Even admins go through
  it on a new install — that's the whole point.
- **Don't hardcode the required-keys list in the frontend.** Read it from
  `setup-status.missingRequired`. Backend can add a required key without a
  frontend redeploy.
- **Don't show plaintext passwords in the integrations step.** The 1C
  password field is encrypted at rest on the backend — `GET /v1/settings`
  returns `********`. UI should treat it as write-only.

## Estimated effort

3–4 days. Bulk of it is the wizard.