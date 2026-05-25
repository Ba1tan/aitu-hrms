# Phase 2 — Attendance & Leave

**Goal:** Employees can check in / submit leave; managers can approve;
HR can see company-wide views and configure holidays + schedules.

## Required reading

1. `docs/API_CONTRACT.md` (Attendance and Leave sections)
2. `docs/PERMISSIONS.md` (`ATTENDANCE_*`, `LEAVE_*` codes)
3. `services/attendance-service/ATTENDANCE_SERVICE.md`
4. `services/leave-service/LEAVE_SERVICE.md`

## Tasks

### Attendance

1. **`client/components/AttendanceWidget.tsx`** — compact check-in/out card for
   the dashboard. Shows today's status + Check-in/Check-out buttons.
   `POST /v1/attendance/check-in` / `check-out`. Disabled outside working hours
   per `company.timezone` (read from `/v1/settings`).
2. **`client/pages/Attendance.tsx`** — replace placeholder. Tabs:
   - **My month** — grid (employees × days for one employee). Pull
     `/v1/attendance/records?from=&to=` with current month.
   - **Team (managers)** — `ATTENDANCE_VIEW_TEAM`. Filter by department.
   - **Company (HR)** — `ATTENDANCE_VIEW_ALL`. Daily view + monthly summary.
3. **`client/pages/AttendanceHolidays.tsx`** — `ATTENDANCE_MANAGE`. CRUD on
   `/v1/attendance/holidays`.
4. **`client/pages/AttendanceSchedules.tsx`** — work schedules CRUD.
5. **Manual entry dialog** — HR can add records via
   `POST /v1/attendance/records`. Shows on the team/company tabs.
6. **Bulk no-show** — `POST /v1/attendance/records/bulk-absent` with a date picker.

### Leave

1. **`client/pages/Leave.tsx`** — replace placeholder. Tabs:
   - **My requests** — `/v1/leave/requests` list with status filter.
     "Submit leave" button opens form: leaveTypeId, startDate, endDate,
     reason. Validate against current balance before POST.
   - **My balances** — `/v1/leave/balances?year=` table: type, total, used,
     remaining, carried-over.
   - **Team calendar (managers)** — Gantt-like view of who's out next 60 days.
2. **`client/pages/LeaveApprovalQueue.tsx`** — `LEAVE_APPROVE_TEAM` or
   `LEAVE_APPROVE_ALL`. The big workflow page:
   - Cards/list of `PENDING` requests in the user's scope
   - Each card: employee name, dates, days, type, reason, "Approve" / "Reject" buttons
   - Reject opens a comment dialog
   - Optimistic update + invalidate `["leave-requests"]`
3. **`client/pages/LeaveTypes.tsx`** — `LEAVE_BALANCE_MANAGE`. CRUD on
   `/v1/leave/types`. Fields: name, code, daysAllowed, paid, requiresApproval,
   carryoverAllowed, carryoverMaxDays.

### Hooks

`client/hooks/api/useAttendance.ts`, `useLeave.ts`. One Query per resource
type; key prefix matches the URL.

## Component checklist

`tabs`, `calendar`, `dialog`, `select`, `badge`, `tooltip`, `popover` (date
range picker), `data-table` pattern (TanStack Table).

## Definition of done

- [ ] An EMPLOYEE-role user can check in/out from the dashboard widget.
- [ ] An EMPLOYEE-role user can submit a leave request and see it pending.
- [ ] A MANAGER role can see only their direct reports in the team view.
- [ ] A MANAGER role can approve/reject in the queue.
- [ ] An HR_MANAGER can do everything plus manage holidays + schedules.
- [ ] Approved leave fans out via `LeaveApprovedEvent` → attendance auto-records
  ON_LEAVE (verify on the broker via `rabbitmqctl list_queues`).
- [ ] `typecheck` + `build:client` green.

## Things to avoid

- **Don't poll for new approvals.** When notification-service ships, it
  will push via WebSocket; for now invalidate on focus
  (`queryClient.invalidateQueries({ queryKey: ["leave-requests"], refetchType: "active" })`).
- **Don't trust frontend balance math.** Always post the request and let
  the backend reject if balance is insufficient — race conditions with
  parallel submissions otherwise.
- **Don't use a single global calendar for all roles** — the Russian-locale
  date-fns + an explicit `weekStartsOn: 1` (Monday) is enough; let the
  page-level filter handle scope.

## Estimated effort

5–7 days. Leave approval queue and team calendar are the time sinks.

---
