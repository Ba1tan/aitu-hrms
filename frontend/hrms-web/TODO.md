# Frontend TODO - Employees Module Implementation

## Current Status
✅ Auth module complete (login/register/refresh/logout)

## Phase 1: Employees Module (In Progress)

### Step 1: API Types & Endpoints ✅ COMPLETE
- ✅ `frontend/shared/api.ts` - Added Employee DTOs + full CRUD endpoints
- ✅ Fixed Position salaries: string (API contract)
- Dependencies: Employee interface, CreateEmployeeRequest, PageResponse<T>

### Step 2: Employees List Page ✅ COMPLETE
- ✅ `frontend/client/pages/EmployeesList.tsx` - Full table + search/filter/pagination
- ✅ shadcn: table, input, select, pagination, card, button, badge
- ✅ React Query + departments prefetch
- ✅ Loading/error states + empty state

### Step 3: Employee Form ✅ COMPLETE
- ✅ `frontend/client/pages/EmployeeForm.tsx` - Full create/edit form
- ✅ shadcn: form, inputs, select, calendar, checkbox, card
- ✅ React Hook Form + Zod validation
- ✅ Full CRUD mutations + queries
- ✅ Responsive grid layout

**Next: Step 4 → Finalize routing + test**

### Step 4: Routing & Hook [PENDING]
- [ ] `frontend/client/App.tsx` - Add /employees route
- [ ] `frontend/client/hooks/useEmployees.ts` - React Query hooks

### Step 5: Test [PENDING]
- [ ] pnpm dev → login → /employees → create → list pagination

## Phase 2: Departments & Positions [LATER]
- [ ] Departments page + CRUD
- [ ] Positions page + CRUD (dept filter)

**Next: Execute Step 1 → api.ts updates**

