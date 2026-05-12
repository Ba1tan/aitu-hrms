# Phase 1 — Core HR

**Goal:** SUPER_ADMIN / HR_MANAGER can fully manage the org chart.
Departments and positions are CRUD-able; employees can be listed with
real filters, viewed in detail, hired, terminated.

## Required reading (do not skip)

1. `docs/API_CONTRACT.md` (repo root) — Employees / Departments / Positions sections
2. `docs/PERMISSIONS.md` (repo root) — `EMPLOYEE_*`, `DEPT_MANAGE` codes
3. `services/employee-service/EMPLOYEE_SERVICE.md` — full endpoint list (38)
4. `frontend/hrms-web/shared/api.ts` — existing `employeesApi`, `departmentsApi`, `positionsApi`
5. `frontend/hrms-web/client/pages/EmployeeForm.tsx` — existing solid form to mirror

## Tasks

### 1. Format helpers (`client/lib/format.ts` — new)

Single source of truth for money/date/IIN/status pill rendering.

```ts
export const formatKZT = (n: number | string) =>
  new Intl.NumberFormat("ru-KZ", {
    style: "currency", currency: "KZT", maximumFractionDigits: 0,
  }).format(typeof n === "string" ? parseFloat(n) : n);

export const formatDate = (iso: string | null | undefined) =>
  iso ? new Date(iso).toLocaleDateString("ru-RU") : "—";

export const maskIin = (iin: string | null | undefined) =>
  iin ? `${iin.slice(0, 6)}••••${iin.slice(-2)}` : "—";

export const statusColor: Record<string, string> = {
  ACTIVE: "#10B981", ON_LEAVE: "#F59E0B",
  PROBATION: "#3B82F6", TERMINATED: "#EF4444", SUSPENDED: "#94A3B8",
};
```

### 2. Hooks library (`client/hooks/api/` — new dir)

One file per domain so cache keys live in one place.

- `useEmployees.ts` — `useEmployees(filters)`, `useEmployee(id)`,
  `useCreateEmployee()`, `useUpdateEmployee()`, `useTerminateEmployee()`,
  `useCreateAccountForEmployee()`.
- `useDepartments.ts` — `useDepartments()`, `useCreateDepartment()`,
  `useUpdateDepartment()`, `useDeleteDepartment()`.
- `usePositions.ts` — same shape.

Use `["employees", filters]` as the React Query key. Invalidate on mutate.

### 3. Departments page (`client/pages/Departments.tsx` — new)

- Table: name, code, manager (lookup employee.fullName), # employees, parent.
- Row actions: edit, delete (with confirm dialog).
- Toolbar: search + "Add department" button gated by `<RequirePermission code="DEPT_MANAGE">`.
- Modal form: name, code, description, parent (tree picker), manager (employee combobox).
- React Hook Form + Zod. Zod schema in `shared/schemas/department.ts` (new).
- Add route in `App.tsx`: `<Route path="/departments" element={<Departments />} />` inside the `ProtectedRoute` block.
- Add to `DashboardLayout` sidebar.

### 4. Positions page (`client/pages/Positions.tsx` — new)

Same pattern. Extra fields: title, departmentId (dropdown from useDepartments),
minSalary, maxSalary, description. Filter table by dept.

### 5. Finish EmployeesList (`client/pages/EmployeesList.tsx` — rewrite)

Current is 64 lines, very thin. Replace with:

- Toolbar: search input (debounced), department filter, status filter, employmentType filter, "Add employee" button (gated by `EMPLOYEE_CREATE`).
- Table columns: photo + name + email, employee#, department, position, status pill, hireDate, actions menu.
- Server-side pagination: `?page&size&search&departmentId&status`.
- Use shadcn `Table`, `Input`, `Select`, `Pagination`, `Badge`, `DropdownMenu`.
- Row click → `/employees/:id` (detail page, see Task 6).
- Skeleton state during load, empty state with CTA, error state with retry.

### 6. Employee detail page (`client/pages/EmployeeDetail.tsx` — new)

Replaces the current `/employees/:id` → EmployeeForm redirect. Tabs:

- **Profile** — read-only view of full record + "Edit" button → opens EmployeeForm in modal/route
- **Salary history** — table of salary_history entries; "Salary change" button (`EMPLOYEE_SALARY_CHANGE` once that permission exists; for now gate with `EMPLOYEE_UPDATE`)
- **Documents** — list + upload + download. Multipart POST to `/v1/employees/{id}/documents`
- **Emergency contacts** — list + CRUD
- **Biometric** — show enrollment status; "Enroll face" button (multipart 3-5 photos) gated by `EMPLOYEE_BIOMETRIC`. ai-ml-service must be up.
- **Leave / Attendance** — embed compact widgets pulling from employee's own data

Route: `<Route path="/employees/:id" element={<EmployeeDetail />} />`.
Move existing EmployeeForm to `/employees/new` and `/employees/:id/edit`.

### 7. Org chart (`client/pages/OrgChart.tsx` — new)

Visualize `/v1/employees/org-chart`. Use [`reactflow`](https://reactflow.dev/)
or a simple custom tree (CSS grid + connectors). Each node = card with
photo + name + position. Click to navigate to employee detail.

### 8. Sidebar updates (`client/pages/DashboardLayout.tsx`)

Add nav items: Departments, Positions, Org chart. Order them sensibly:
Dashboard → Employees → Org chart → Departments → Positions → Payroll → Leaves → Attendance → Reports.

Wrap each in `<RequirePermission>` so EMPLOYEE-role users don't see admin menus.

### 9. Hire / terminate workflows

- **Hire**: existing `POST /v1/employees` already creates the row. After
  success, prompt: "Also create a user account?" → calls
  `POST /v1/employees/{id}/create-account`. employee-service publishes
  `EmployeeCreatedEvent` → user-service auto-provisions account.
- **Terminate**: `POST /v1/employees/{id}/terminate` with `{ terminationDate, reason }`.
  Confirm dialog with date picker. After success, refresh the row.

## Component checklist

These shadcn primitives must exist; install if missing:

- `table`, `dialog`, `dropdown-menu`, `popover`, `command` (for combobox),
  `tabs`, `badge`, `pagination`, `skeleton`, `select`, `calendar`,
  `alert-dialog` (confirms), `tooltip`.

All 49 are already in `client/components/ui/` per Phase 0 audit; verify
none are stub-only by reading the file size.

## Definition of done

- [ ] SUPER_ADMIN can create / list / edit / delete a department.
- [ ] SUPER_ADMIN can create / list / edit / delete a position.
- [ ] EmployeesList shows real server-paged data with working search + filters.
- [ ] EmployeeDetail renders Profile/Salary/Documents/Emergency/Biometric tabs.
- [ ] Org chart renders for at least 10 employees.
- [ ] Hire flow ends with a user account created (verify via Bruno test).
- [ ] Terminate flow soft-deletes + records terminationDate.
- [ ] EMPLOYEE-role user can only see /dashboard + /employees/{ownId}.
- [ ] `npx pnpm typecheck` + `npx pnpm run build:client` both green.
- [ ] Manual smoke against staging (192.168.100.53:8091) confirms 7-step flow works end-to-end.

## Things to avoid

- **Don't add a fake "Add user" button on the Employees page.** Users are
  created automatically via the `EmployeeCreatedEvent` listener when an
  employee is created with an email (see `bruno/02-Setup/08`).
- **Don't query `/v1/users` for employee names** — that's user-service.
  Employee names are on the employee record itself.
- **IIN validation:** 12 digits + mod-11 checksum. The backend rejects bad
  ones (`Invalid IIN check digit`). Mirror the rule in Zod for instant
  feedback — algorithm in `services/employee-service/.../util/IinValidator.java`.
- **Don't fetch all employees for a combobox** — use the existing
  `/v1/employees?search=` endpoint as the data source (TanStack Query + debounce).

## Estimated effort

4–6 days for one developer comfortable with React Query + shadcn. Bulk of
the work is the EmployeeDetail tabs.

---

## Phase 1B — Admin section (extension)

These pages are user-service-backed and only relevant to SUPER_ADMIN +
ACCOUNTANT / HR for narrow slices. Treat them as a subroute family under
`/admin/*` so the sidebar can hide the whole group in one place.

### 10. User management (`client/pages/admin/Users.tsx`)

`SYSTEM_USERS` permission.

- Table: email, name, role pill, status (enabled / locked), employee link
  (employeeId or "—"), lastLoginAt, actions.
- Toolbar: search, role filter, status filter, "Create user" button.
- Row actions: edit role, lock / unlock, link to employee, reset password,
  soft-delete.
- Create form: `POST /v1/users` (already exists in api.ts as `apiClient.post`).
- Edit role: `PUT /v1/users/{id}` with `{ role }`.
- Lock / unlock: `PUT /v1/users/{id}` with `{ accountNonLocked }`.
- Link to employee: `PUT /v1/users/{id}/link-employee` with `{ employeeId }`.
  Combobox over `/v1/employees?search=`.
- Password reset (admin-triggered): `POST /v1/auth/forgot-password` with
  the user's email; backend emails the reset link.

### 11. Audit log viewer (`client/pages/admin/AuditLog.tsx`)

`SYSTEM_AUDIT`. Backend table: `hrms_user.audit_logs`. Endpoint
`GET /v1/users/audit` (needs adding on the backend if missing — flag in
PR description; UI can use placeholder data until then).

- Table: timestamp, actor (email), action (CREATE / UPDATE / DELETE / APPROVE),
  entityType, entityId, IP.
- Row click → side-panel with full JSONB old/new diff. Use `react-diff-viewer`
  or build a simple two-column key-value highlight.
- Filters: actor combobox, entityType select, action select, date range picker.
- Export to CSV button.

### 12. Role editor (`client/pages/admin/Roles.tsx`)

`SYSTEM_ROLES`. Read-only matrix for v1: rows = roles, columns =
permissions grouped by module. Checkmarks where granted.

- Data source: `/v1/users/roles` and `/v1/users/permissions` (backend
  endpoints to add if missing — flag in PR).
- Edit mode: SUPER_ADMIN only. Toggling a checkbox calls
  `POST /v1/users/roles/{role}/permissions` with `{ add: [...], remove: [...] }`
  (backend contract TBD).
- Important: changes are reflected in newly-issued JWTs only. Existing
  sessions keep their old permission list until token refresh. Show a
  banner on save: "Affected users must log out and back in."

### Admin sidebar

Add a collapsible "Admin" group at the bottom of the sidebar containing
the three pages above. Wrap the whole group in
`<RequirePermission anyOf={["SYSTEM_USERS", "SYSTEM_AUDIT", "SYSTEM_ROLES"]}>`.

### Definition of done (Phase 1B)

- [ ] SUPER_ADMIN can create + edit + lock + delete a user
- [ ] SUPER_ADMIN can link a user to an employee
- [ ] Audit log viewer renders with timestamps + actor (placeholder rows OK if
  backend endpoint doesn't exist yet)
- [ ] Role matrix renders read-only
- [ ] All three pages are hidden for non-admin roles