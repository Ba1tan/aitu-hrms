# HRMS API Contract
**Version:** 1.0.0 | **Base URL:** `https://hrms.nursnerv.uk/api` | **Local:** `http://localhost:8080/api`

All requests require `Authorization: Bearer <access_token>` unless marked **Public**.  
All responses are wrapped: `{ "success": true, "message": "...", "data": <T>, "timestamp": "..." }`.  
All IDs are UUID strings. All monetary values are decimal strings (e.g. `"245000.00"`).

---

## Table of Contents
1. [Authentication](#1-authentication)
2. [Employees](#2-employees)
3. [Departments & Positions](#3-departments--positions)
4. [Payroll Periods](#4-payroll-periods)
5. [Payslips](#5-payslips)
6. [Leave](#6-leave)
7. [Attendance](#7-attendance)
8. [Notifications](#8-notifications)
9. [Reports](#9-reports)
10. [Error Reference](#10-error-reference)
11. [Roles & Permissions](#11-roles--permissions)

---

## 1. Authentication

### POST `/auth/login` — **Public**

**Request:**
```json
{
  "email": "admin@hrms.kz",
  "password": "your_password"
}
```

**Response `200`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "admin@hrms.kz",
    "firstName": "Nursultan",
    "lastName": "Admin",
    "role": "SUPER_ADMIN",
    "employeeId": null
  }
}
```

**Errors:** `401` Invalid credentials | `400` Validation error

---

### POST `/auth/refresh` — **Public**

**Request:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

**Response `200`:** Same shape as `/auth/login`

**Errors:** `400` Token expired or invalid

---

### POST `/auth/logout` — Authenticated

**Request header:** `Authorization: Bearer <access_token>`  
**Response `200`:** `{ "message": "Logged out successfully", "data": null }`

---

### POST `/auth/change-password` — Authenticated

**Request:**
```json
{
  "currentPassword": "old_password",
  "newPassword": "new_password_min8chars"
}
```

**Response `200`:** `{ "message": "Password changed successfully", "data": null }`

**Errors:** `400` Current password incorrect

---

### POST `/auth/register` — `SUPER_ADMIN` only

Creates a new user account. Employee link (`employeeId`) can be set later via employee update.

**Request:**
```json
{
  "firstName": "Асель",
  "lastName": "Нурова",
  "email": "asel.nurova@company.kz",
  "password": "temp_password123",
  "role": "HR_MANAGER"
}
```

**Valid roles:** `SUPER_ADMIN` | `HR_MANAGER` | `ACCOUNTANT` | `MANAGER` | `EMPLOYEE`

**Response `201`:** Same shape as `/auth/login`

**Errors:** `400` Email already registered | `400` Invalid role

---

## 2. Employees

### GET `/v1/employees` — `HR_MANAGER`, `ACCOUNTANT`, `MANAGER`

**Query params:**
| Param | Type | Description |
|-------|------|-------------|
| `search` | string | Search by name, email, or employee number |
| `departmentId` | UUID | Filter by department |
| `status` | enum | `ACTIVE` \| `ON_LEAVE` \| `TERMINATED` \| `PROBATION` |
| `page` | int | 0-indexed, default `0` |
| `size` | int | Default `20` |
| `sort` | string | Default `lastName` |

**Response `200`:**
```json
{
  "content": [
    {
      "id": "a1b2c3d4-...",
      "employeeNumber": "EMP-0042",
      "firstName": "Асель",
      "lastName": "Нурова",
      "fullName": "Нурова Асель",
      "email": "asel.nurova@company.kz",
      "status": "ACTIVE",
      "department": { "id": "...", "name": "Разработка" },
      "position": { "id": "...", "title": "Backend Developer" }
    }
  ],
  "totalElements": 87,
  "totalPages": 5,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false
}
```

---

### GET `/v1/employees/{id}` — Authenticated

**Response `200`:**
```json
{
  "id": "a1b2c3d4-...",
  "employeeNumber": "EMP-0042",
  "firstName": "Асель",
  "lastName": "Нурова",
  "middleName": "Бекова",
  "fullName": "Нурова Асель Бекова",
  "email": "asel.nurova@company.kz",
  "iin": "950312300145",
  "phone": "+7 701 234 5678",
  "hireDate": "2023-03-01",
  "terminationDate": null,
  "dateOfBirth": "1995-03-12",
  "status": "ACTIVE",
  "employmentType": "FULL_TIME",
  "baseSalary": "320000.00",
  "department": { "id": "...", "name": "Разработка" },
  "position": { "id": "...", "title": "Backend Developer" },
  "manager": { "id": "...", "fullName": "Нурсултан Тореханов", "email": "n.torekhanov@company.kz" },
  "bankAccount": "KZ91722C000001234567",
  "bankName": "Халык Банк",
  "resident": true,
  "hasDisability": false,
  "pensioner": false,
  "createdAt": "2023-03-01T09:00:00",
  "updatedAt": "2024-01-15T14:30:00"
}
```

**Errors:** `404` Employee not found

---

### POST `/v1/employees` — `HR_MANAGER`

**Request:**
```json
{
  "firstName": "Асель",
  "lastName": "Нурова",
  "middleName": "Бекова",
  "email": "asel.nurova@company.kz",
  "iin": "950312300145",
  "phone": "+7 701 234 5678",
  "hireDate": "2024-02-01",
  "dateOfBirth": "1995-03-12",
  "employmentType": "FULL_TIME",
  "baseSalary": "320000.00",
  "departmentId": "dept-uuid",
  "positionId": "pos-uuid",
  "managerId": "manager-uuid",
  "bankAccount": "KZ91722C000001234567",
  "bankName": "Халык Банк",
  "resident": true,
  "hasDisability": false,
  "pensioner": false
}
```

**`employmentType` values:** `FULL_TIME` | `PART_TIME` | `CONTRACT` | `INTERN`

**Response `201`:** Full employee object (same as GET by id)

**Errors:** `400` Email already exists | `400` IIN already exists | `404` Department/Position/Manager not found

---

### PUT `/v1/employees/{id}` — `HR_MANAGER`

All fields optional — only provided fields are updated.

**Request:** Same shape as POST, all fields optional.

**Response `200`:** Updated employee object.

---

### PATCH `/v1/employees/{id}/status` — `HR_MANAGER`

**Request:**
```json
{
  "status": "TERMINATED",
  "terminationDate": "2024-12-31"
}
```

> `terminationDate` **required** when `status = TERMINATED`.

**`status` values:** `ACTIVE` | `ON_LEAVE` | `TERMINATED` | `PROBATION`

**Response `200`:** Updated employee object.

---

### DELETE `/v1/employees/{id}` — `SUPER_ADMIN`

Soft delete — employee is hidden from all lists but data is preserved.  
**Response `200`:** `{ "message": "Employee deleted" }`

---

## 3. Departments & Positions

### GET `/v1/departments` — Authenticated
Returns flat list of all departments.

**Response `200`:**
```json
[
  {
    "id": "...",
    "name": "Разработка",
    "description": "Backend and frontend development",
    "costCenter": "CC-001",
    "parent": { "id": "...", "name": "Технический отдел" },
    "manager": { "id": "...", "fullName": "Нурсултан Тореханов", "email": "..." },
    "employeeCount": 12
  }
]
```

### POST `/v1/departments` — `HR_MANAGER`
```json
{ "name": "Разработка", "description": "...", "costCenter": "CC-001", "parentId": null, "managerId": "uuid" }
```
**Response `201`:** Department object.

### PUT `/v1/departments/{id}` — `HR_MANAGER`  
### DELETE `/v1/departments/{id}` — `SUPER_ADMIN`

---

### GET `/v1/positions` — Authenticated

**Query:** `?departmentId=uuid` — optional filter

**Response `200`:**
```json
[
  {
    "id": "...",
    "title": "Backend Developer",
    "description": "...",
    "minSalary": "250000.00",
    "maxSalary": "500000.00",
    "department": { "id": "...", "name": "Разработка" }
  }
]
```

### POST `/v1/positions` — `HR_MANAGER`
```json
{ "title": "Backend Developer", "minSalary": "250000.00", "maxSalary": "500000.00", "departmentId": "uuid" }
```

### PUT `/v1/positions/{id}` — `HR_MANAGER`  
### DELETE `/v1/positions/{id}` — `SUPER_ADMIN`

---

## 4. Payroll Periods

### GET `/v1/payroll/periods` — `HR_MANAGER`, `ACCOUNTANT`, `MANAGER`

**Query:** `?page=0&size=12`

**Response `200`:**
```json
{
  "content": [
    {
      "id": "period-uuid",
      "year": 2024,
      "month": 3,
      "name": "Март 2024",
      "startDate": "2024-03-01",
      "endDate": "2024-03-31",
      "workingDays": 20,
      "status": "PAID",
      "summary": {
        "payslipCount": 87,
        "approvedCount": 87,
        "totalGrossSalary": "27840000.00",
        "totalNetSalary": "22514280.00",
        "totalIpn": "2497200.00",
        "totalOpv": "2784000.00",
        "totalSo": "828456.00"
      },
      "createdAt": "2024-03-25T10:00:00",
      "updatedAt": "2024-03-29T16:45:00"
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "size": 12,
  "number": 0
}
```

**`status` values:** `DRAFT` → `PROCESSING` → `APPROVED` → `PAID` → `LOCKED`

---

### GET `/v1/payroll/periods/{periodId}` — `HR_MANAGER`, `ACCOUNTANT`, `MANAGER`

**Response `200`:** Single period object (same shape as list item).

---

### POST `/v1/payroll/periods` — `HR_MANAGER`

**Request:**
```json
{ "year": 2024, "month": 4, "workingDays": 22 }
```

**Response `201`:** Period object with status `DRAFT`.

**Errors:** `400` Period already exists for this year+month.

---

### POST `/v1/payroll/periods/{periodId}/generate` — `HR_MANAGER`

Generates payslips for all active employees. If `employeeIds` is empty, generates for everyone.

**Request (optional body):**
```json
{ "employeeIds": ["uuid1", "uuid2"] }
```

**Response `200`:**
```json
{
  "generated": 87,
  "skipped": 2,
  "errors": 0,
  "totalGrossPayout": "27840000.00",
  "totalNetPayout": "22514280.00",
  "errorDetails": []
}
```

Period status changes `DRAFT` → `PROCESSING` automatically.

**Errors:** `400` Period is LOCKED or PAID.

---

### POST `/v1/payroll/periods/{periodId}/approve` — `HR_MANAGER`

Approves all DRAFT payslips. Period moves `PROCESSING` → `APPROVED`.

**Response `200`:** Updated period object.

**Errors:** `400` Period not in PROCESSING status | `400` No payslips generated yet.

---

### POST `/v1/payroll/periods/{periodId}/mark-paid` — `ACCOUNTANT`

Period moves `APPROVED` → `PAID`. All payslips set to PAID.

**Response `200`:** Updated period object.

---

### POST `/v1/payroll/periods/{periodId}/lock` — `SUPER_ADMIN`

Period moves `PAID` → `LOCKED`. Immutable archive — nothing can be changed.

**Response `200`:** Updated period object.

---

## 5. Payslips

### GET `/v1/payroll/periods/{periodId}/payslips` — `HR_MANAGER`, `ACCOUNTANT`

**Query:** `?page=0&size=50`

**Response `200`:** Paginated list of payslip summaries.

**Single payslip shape:**
```json
{
  "id": "payslip-uuid",
  "period": { "id": "...", "name": "Март 2024", "year": 2024, "month": 3 },
  "employee": {
    "id": "...",
    "employeeNumber": "EMP-0042",
    "fullName": "Нурова Асель",
    "email": "asel.nurova@company.kz",
    "department": "Разработка",
    "position": "Backend Developer"
  },
  "workedDays": 20,
  "totalWorkingDays": 20,
  "grossSalary": "320000.00",
  "earnedSalary": "320000.00",
  "allowances": "0.00",
  "opvAmount": "32000.00",
  "oopvAmount": "0.00",
  "taxableIncome": "284308.00",
  "ipnAmount": "28430.80",
  "otherDeductions": "0.00",
  "totalDeductions": "60430.80",
  "netSalary": "259569.20",
  "soAmount": "10080.00",
  "snAmount": "20369.96",
  "mrpUsed": 3692,
  "resident": true,
  "status": "APPROVED",
  "pdfUrl": null,
  "createdAt": "2024-03-26T14:00:00"
}
```

> **Tax breakdown legend:**  
> `opvAmount` = ОПВ (pension, 10%, employee deduction)  
> `ipnAmount` = ИПН (income tax, 10%, employee deduction)  
> `soAmount` = СО (social contribution, 3.5%, employer cost — not deducted)  
> `snAmount` = СН (social tax, 9.5% - СО, employer cost — not deducted)  
> `netSalary` = earnedSalary − opv − oopv − ipn + allowances − otherDeductions

---

### GET `/v1/payroll/payslips/{payslipId}` — `HR_MANAGER`, `ACCOUNTANT`

**Response `200`:** Single payslip (same shape).

---

### PATCH `/v1/payroll/payslips/{payslipId}/adjust` — `HR_MANAGER`

Only works on `DRAFT` payslips. Recalculates automatically after adjustment.

**Request (all fields optional):**
```json
{
  "workedDays": 18,
  "allowances": "25000.00",
  "otherDeductions": "5000.00"
}
```

**Response `200`:** Recalculated payslip.

**Errors:** `400` Payslip not in DRAFT status.

---

### GET `/v1/payroll/my-payslips` — Authenticated

Employee self-service — returns own payslips only.

**Query:** `?page=0&size=12`

**Response `200`:** Paginated list (same payslip shape, period newest first).

---

### GET `/v1/payroll/my-payslips/period/{periodId}` — Authenticated

**Response `200`:** Single payslip for that period.

**Errors:** `404` No payslip for this employee in this period.

---

## 6. Leave

### GET `/v1/leave/types` — Authenticated

Returns the 5 seeded leave types from Kazakhstan Labour Code.

**Response `200`:**
```json
[
  { "id": "uuid1", "name": "Annual Leave", "daysAllowed": 24, "paid": true, "description": "Ежегодный оплачиваемый отпуск (Art. 88)" },
  { "id": "uuid2", "name": "Sick Leave", "daysAllowed": 30, "paid": true, "description": "Больничный лист" },
  { "id": "uuid3", "name": "Maternity Leave", "daysAllowed": 126, "paid": true, "description": "Декретный отпуск" },
  { "id": "uuid4", "name": "Unpaid Leave", "daysAllowed": 14, "paid": false, "description": "Отпуск без сохранения зарплаты" },
  { "id": "uuid5", "name": "Study Leave", "daysAllowed": 10, "paid": true, "description": "Учебный отпуск" }
]
```

---

### GET `/v1/leave/requests` — `HR_MANAGER`

All requests, paginated.  
**Query:** `?page=0&size=20`

**Response `200`:** Paginated list of leave request objects.

**Single leave request shape:**
```json
{
  "id": "req-uuid",
  "employee": {
    "id": "...", "employeeNumber": "EMP-0042",
    "fullName": "Нурова Асель", "email": "...",
    "status": "ACTIVE",
    "department": { "id": "...", "name": "Разработка" },
    "position": { "id": "...", "title": "Backend Developer" }
  },
  "leaveType": { "id": "...", "name": "Annual Leave", "daysAllowed": 24, "paid": true },
  "startDate": "2024-04-10",
  "endDate": "2024-04-15",
  "daysRequested": 4,
  "reason": "Семейный отпуск",
  "status": "PENDING",
  "reviewedBy": null,
  "reviewedAt": null,
  "reviewComment": null,
  "createdAt": "2024-04-01T09:15:00"
}
```

**`status` values:** `PENDING` | `APPROVED` | `REJECTED` | `CANCELLED`

---

### GET `/v1/leave/requests/my` — Authenticated

Own requests, paginated.  
**Query:** `?page=0&size=20`

---

### GET `/v1/leave/requests/pending` — `MANAGER`, `HR_MANAGER`

Returns list (not paginated) of pending requests for the authenticated manager's direct reports.

---

### POST `/v1/leave/requests` — `EMPLOYEE`

**Request:**
```json
{
  "leaveTypeId": "uuid-of-annual-leave",
  "startDate": "2024-04-10",
  "endDate": "2024-04-15",
  "daysRequested": 4,
  "reason": "Семейный отпуск"
}
```

> `daysRequested` must be calculated by the frontend (Mon–Fri only). Backend validates it matches the date range.

**Response `201`:** Leave request with status `PENDING`.

**Errors:**  
`400` Insufficient leave balance (`"You have 3 days remaining, requested 4"`)  
`400` Overlapping request exists  
`400` No leave balance found for this type/year

---

### PUT `/v1/leave/requests/{id}/approve` — `MANAGER`, `HR_MANAGER`

**Request (optional):**
```json
{ "comment": "Одобрено" }
```

**Response `200`:** Updated request with status `APPROVED`.

**Errors:** `400` Request not in PENDING status | `403` Manager is not this employee's manager

---

### PUT `/v1/leave/requests/{id}/reject` — `MANAGER`, `HR_MANAGER`

**Request:**
```json
{ "comment": "Недостаточно сотрудников в период" }
```

**Response `200`:** Updated request with status `REJECTED`.

---

### PUT `/v1/leave/requests/{id}/cancel` — `EMPLOYEE`

Can only cancel own PENDING requests.

**Response `200`:** Updated request with status `CANCELLED`.

**Errors:** `403` Not own request | `400` Request already approved/rejected

---

### GET `/v1/leave/balances/my` — Authenticated

Own balances for the current year.

**Response `200`:**
```json
[
  {
    "id": "...",
    "leaveType": { "id": "...", "name": "Annual Leave", "daysAllowed": 24, "paid": true },
    "year": 2024,
    "entitledDays": 24,
    "usedDays": 8,
    "remainingDays": 16
  }
]
```

---

### GET `/v1/leave/balances/{employeeId}` — `HR_MANAGER`

Same shape for any employee.

---

### POST `/v1/leave/balances/initialize` — `SUPER_ADMIN`

**Query:** `?year=2025`

Initializes leave balances for all active employees for the given year (idempotent — skips existing).

**Response `200`:** `{ "message": "Balances initialized" }`

---

## 7. Attendance

### POST `/v1/attendance/check-in` — `EMPLOYEE`

Records today's check-in for the authenticated employee.

**Request:** No body.

**Response `201`:**
```json
{
  "id": "att-uuid",
  "employee": { "id": "...", "fullName": "Нурова Асель", ... },
  "workDate": "2024-04-15",
  "checkIn": "2024-04-15T09:07:00",
  "checkOut": null,
  "workedHours": null,
  "status": "LATE",
  "note": null,
  "createdAt": "2024-04-15T09:07:00"
}
```

**`status` after check-in:** `PRESENT` (on time) or `LATE` (>10 min after 09:00)

**Errors:** `400` Already checked in today

---

### POST `/v1/attendance/check-out` — `EMPLOYEE`

**Request:** No body.

**Response `200`:** Updated record with `checkOut` and `workedHours` set.  
Status may change to `HALF_DAY` if `workedHours < 4`.

**Errors:** `400` No check-in found for today | `400` Already checked out

---

### GET `/v1/attendance/today` — Authenticated

**Response `200`:** Today's attendance record, or `null` if not yet checked in.

---

### GET `/v1/attendance/my` — Authenticated

Own monthly records.  
**Query:** `?month=4&year=2024`

**Response `200`:** Array of attendance records for that month.

---

### GET `/v1/attendance/employees/{employeeId}/monthly` — `HR_MANAGER`, `MANAGER`

**Query:** `?month=4&year=2024`

**Response `200`:** Array of attendance records.

---

### POST `/v1/attendance/manual` — `HR_MANAGER`

Manual entry for backdating or corrections.

**Request:**
```json
{
  "employeeId": "uuid",
  "workDate": "2024-04-12",
  "checkIn": "2024-04-12T09:00:00",
  "checkOut": "2024-04-12T18:00:00",
  "status": "PRESENT",
  "note": "Внесено вручную по причине сбоя системы"
}
```

**Response `201`:** Attendance record.

**Errors:** `400` Record already exists for that date

---

### GET `/v1/attendance` — `HR_MANAGER`

All employees, filtered by date and/or employee.  
**Query:** `?date=2024-04-15&employeeId=uuid` (both optional)

**Response `200`:** Array of attendance records.

---

## 8. Notifications

### GET `/v1/notifications` — Authenticated

Own notifications, newest first.  
**Query:** `?page=0&size=20`

**Response `200`:**
```json
{
  "content": [
    {
      "id": "notif-uuid",
      "type": "LEAVE_APPROVED",
      "title": "Отпуск одобрен",
      "body": "Ваш запрос на отпуск 10.04–15.04 одобрен",
      "referenceId": "leave-request-uuid",
      "referenceType": "LEAVE_REQUEST",
      "read": false,
      "readAt": null,
      "createdAt": "2024-04-02T10:30:00"
    }
  ],
  "totalElements": 5, ...
}
```

**Notification types:** `LEAVE_REQUEST` | `LEAVE_APPROVED` | `LEAVE_REJECTED` | `LEAVE_CANCELLED` | `PAYSLIP_READY` | `PAYROLL_PROCESSED` | `ATTENDANCE_ALERT` | `SYSTEM`

---

### GET `/v1/notifications/unread-count` — Authenticated

**Response `200`:** `{ "data": 3 }` (integer)

Poll this every 30s for notification badge.

---

### PUT `/v1/notifications/{id}/read` — Authenticated

**Response `200`:** `{ "message": "Marked as read" }`

---

### PUT `/v1/notifications/read-all` — Authenticated

**Response `200`:** `{ "message": "All marked as read" }`

---

## 9. Reports (file downloads)

All report endpoints return binary file content, not JSON.

**Response headers:**
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="report_name.xlsx"
```

**Frontend pattern:**
```typescript
const response = await client.get('/v1/reports/payroll-summary', {
  params: { periodId },
  responseType: 'blob'
})
const url = URL.createObjectURL(response.data)
const a = document.createElement('a')
a.href = url; a.download = 'payroll_march_2024.xlsx'; a.click()
URL.revokeObjectURL(url)
```

---

### GET `/v1/reports/payroll-summary` — `HR_MANAGER`, `ACCOUNTANT`

**Query:** `?periodId=uuid`

Returns XLSX: one row per employee with all tax breakdown columns.

---

### GET `/v1/reports/form-200` — `HR_MANAGER`, `ACCOUNTANT`

**Query:** `?quarter=1&year=2024` (quarter: 1–4)

Returns XLSX: quarterly KZ tax declaration (Form 200.00 layout).

---

### GET `/v1/reports/attendance` — `HR_MANAGER`, `MANAGER`

**Query:** `?from=2024-04-01&to=2024-04-30`

Returns XLSX: attendance summary per employee for the date range.

---

### GET `/v1/reports/leave-summary` — `HR_MANAGER`

**Query:** `?year=2024`

Returns XLSX: leave balance and usage per employee for the year.

---

## 10. Error Reference

All errors follow this shape:
```json
{
  "success": false,
  "message": "Human-readable error description",
  "data": null,
  "errors": { "field": "Validation message" }
}
```

| HTTP Status | When |
|-------------|------|
| `400` | Business rule violation (`BusinessException`) or validation error |
| `401` | Missing or invalid/expired JWT token |
| `403` | Authenticated but insufficient role (`@PreAuthorize` failed) |
| `404` | Entity not found (`ResourceNotFoundException`) |
| `409` | Conflict — duplicate resource (email, IIN, period) |
| `500` | Unexpected server error — check backend logs |

---

## 11. Roles & Permissions

| Role | Description |
|------|-------------|
| `SUPER_ADMIN` | Full access to everything including user registration and locking periods |
| `HR_MANAGER` | Employee CRUD, payroll generation/approval, leave final approval, all reports |
| `ACCOUNTANT` | Read-only payroll, mark periods as paid, download reports |
| `MANAGER` | Read team employees, approve/reject leave for direct reports, read payroll periods |
| `EMPLOYEE` | Own payslips, submit leave requests, check-in/out, own notifications |

### Endpoint Permission Matrix

| Endpoint group | SUPER_ADMIN | HR_MANAGER | ACCOUNTANT | MANAGER | EMPLOYEE |
|----------------|:-----------:|:----------:|:----------:|:-------:|:--------:|
| Auth (login/refresh/logout) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Register user | ✅ | — | — | — | — |
| Employee CRUD | ✅ | ✅ | — | — | — |
| Employee read list | ✅ | ✅ | ✅ | ✅ | — |
| Employee read single | ✅ | ✅ | ✅ | ✅ | own |
| Payroll periods (read) | ✅ | ✅ | ✅ | ✅ | — |
| Payroll generate/approve | ✅ | ✅ | — | — | — |
| Mark period paid | ✅ | — | ✅ | — | — |
| Lock period | ✅ | — | — | — | — |
| Payslip adjust | ✅ | ✅ | — | — | — |
| My payslips | ✅ | ✅ | ✅ | ✅ | ✅ |
| Leave requests (all) | ✅ | ✅ | — | — | — |
| Leave requests (pending) | ✅ | ✅ | — | ✅ | — |
| Submit leave request | ✅ | ✅ | ✅ | ✅ | ✅ |
| Approve/reject leave | ✅ | ✅ | — | ✅ | — |
| My leave / balances | ✅ | ✅ | ✅ | ✅ | ✅ |
| Initialize leave balances | ✅ | — | — | — | — |
| Attendance check-in/out | ✅ | ✅ | ✅ | ✅ | ✅ |
| Manual attendance | ✅ | ✅ | — | — | — |
| Attendance read all | ✅ | ✅ | — | ✅ | — |
| Reports (download) | ✅ | ✅ | ✅ | attendance | — |
| Notifications | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## Quick Reference — Frontend Integration Notes

1. **Token storage:** store `accessToken` + `refreshToken` in `localStorage` (or Zustand persist). Attach as `Authorization: Bearer <token>` header.

2. **Auto-refresh:** on any `401` response, POST `/auth/refresh` with the stored refreshToken, update stored tokens, retry original request once.

3. **Money fields:** always `string` (e.g. `"320000.00"`). Parse with `parseFloat()` only for display math. Use `Intl.NumberFormat` with `currency: 'KZT'` for display.

4. **Date fields:** always ISO-8601 strings (`"2024-04-15"` for dates, `"2024-04-15T09:07:00"` for datetimes). Use `dayjs` for parsing and formatting.

5. **Pagination:** all list endpoints return Spring's `Page<T>` shape — use `content[]`, `totalElements`, `number` (0-indexed page), `size`.

6. **File downloads:** set `responseType: 'blob'` on axios, then `URL.createObjectURL()` to trigger browser download.

7. **Leave days calculation:** the frontend must calculate `daysRequested` (Mon–Fri only, excluding the dates) before submitting. Backend validates the number matches.

8. **Swagger UI:** available at `https://hrms.nursnerv.uk/api/swagger-ui.html` (or local `/api/swagger-ui.html`) — auto-generated from code, always in sync with actual endpoints.
