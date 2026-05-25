# Reporting Service

**Port:** 8087 | **Schema:** hrms_reporting (materialized views only) | **Owner:** Nursultan

## Responsibility
Generate downloadable XLSX/PDF reports **and** serve the role-aware dashboard
aggregation endpoint. No business logic — reads data from other services via
Feign, formats into JSON (dashboard) or documents (reports). Apache POI for
XLSX, OpenPDF for PDF.

## Endpoints (13)

```
# Payroll Reports (REPORT_PAYROLL)
GET /v1/reports/payroll-summary         ?periodId= → XLSX (all employees, all tax columns)
GET /v1/reports/payroll-summary/pdf     ?periodId= → PDF
GET /v1/reports/form200                 ?year=&quarter= → XLSX (quarterly KZ tax declaration)
GET /v1/reports/salary-breakdown        ?departmentId= → XLSX (dept salary stats)

# Attendance Reports (REPORT_ATTENDANCE)
GET /v1/reports/attendance-monthly      ?year=&month= → XLSX (grid: employees × days)
GET /v1/reports/attendance-summary      ?year=&month= → XLSX (present/absent/late counts)

# Leave Reports (REPORT_LEAVE)
GET /v1/reports/leave-balances          ?year= → XLSX (all employees, all leave types)

# HR Reports
GET /v1/reports/employee-directory      → XLSX (all employee fields)
GET /v1/reports/turnover                ?year= → XLSX (hires/terms per month)
GET /v1/reports/headcount               ?from=&to= → XLSX (count by dept/status/type over time)

# Executive (REPORT_EXECUTIVE)
GET /v1/reports/executive-summary       ?year=&month= → PDF (all-in-one)


# Dashboard (any authenticated)
GET /v1/dashboard/stats                 → JSON, role-aware aggregation
```

## Dashboard endpoint detail

`GET /v1/dashboard/stats` returns a single JSON object with role-filtered
sections. Inherited from the monolith's `DashboardController`. Permission:
any authenticated user — fields visible to the caller depend on their role.

| Section | Roles | Fields |
|---------|-------|--------|
| Employees | all except EMPLOYEE | `totalEmployees`, `activeEmployees`, `onLeaveEmployees`, `newHiresThisMonth` |
| Last payroll | SUPER_ADMIN, HR_MANAGER, ACCOUNTANT | `lastPayrollPeriodName`, `lastPayrollStatus`, `lastPayrollGross`, `lastPayrollNet`, `lastPayrollEmployeeCount` |
| Leave | SUPER_ADMIN, HR_MANAGER, MANAGER | `pendingLeaveRequests` |
| Attendance (today) | SUPER_ADMIN, HR_MANAGER, MANAGER | `todayPresent`, `todayAbsent`, `todayLate`, `todayTotal`, `attendanceRate` |
| Personal | any user with an `employeeId` claim | `myLastNetSalary`, `myLastPayrollPeriod`, `myLeaveBalance`, `myAttendanceTodayStatus` |

### Implementation notes

- Each section is built independently; one slow/failing upstream call must not
  break the whole response — wrap each block in try/catch and let missing
  sections come back as `null`. Match the monolith's behaviour.
- All upstream calls are **GET-only via Feign**:
  - `employee-service` → counts (`countByDeletedFalse`, by status, by hireDate ≥ first-of-month)
  - `payroll-service` → `findTopByStatusNot(LOCKED)` for the latest non-locked period; `getPeriodTotals(periodId)` for gross/net/count; `findMyPayslips(employeeId, top 1)` for personal
  - `leave-service` → count of pending leave requests; `findByEmployeeIdAndYear` for personal balance (Annual Leave)
  - `attendance-service` → today's `(present, absent, late, total)` counts; today's record for personal
- Cache the aggregate per `(userId, role)` for ~30s in Redis to absorb dashboard reload bursts. Cache key: `dashboard:stats:{userId}:{epochMinute/2}`.
- Source of legacy implementation: `monolith/src/main/java/kz/aitu/hrms/modules/dashboard/service/DashboardServiceImpl.java` at git SHA `b07cd2c` (last commit before monolith deletion). Use `git show b07cd2c:monolith/src/main/java/kz/aitu/hrms/modules/dashboard/service/DashboardServiceImpl.java` to retrieve it.

## Response Pattern
```java
response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
response.setHeader("Content-Disposition", "attachment; filename=report.xlsx");
reportService.generate(params, response.getOutputStream());
```

## Feign Clients
- `payroll-service` → payslip data for payroll reports + dashboard payroll/personal sections
- `employee-service` → employee list for directory/headcount + dashboard employee counts
- `attendance-service` → attendance records for monthly grid + dashboard today's counts
- `leave-service` → leave balances + dashboard pending count and personal balance

## Events Consumed
- `PayrollJobCompletedEvent` → optionally pre-generate payroll summary report; invalidate dashboard payroll cache
