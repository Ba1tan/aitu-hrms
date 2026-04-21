# HRMS API Contract — Complete Frontend Reference

**Version:** 2.0 | **Base URL:** `https://hrms.nursnerv.uk/api` | **Local:** `http://localhost:8080/api`  
**For:** Nurbol (Frontend Developer)

All requests require `Authorization: Bearer <access_token>` unless marked **Public**.  
All responses wrapped: `{ "success": true, "message": "...", "data": <T>, "timestamp": "..." }`  
All IDs: UUID strings. All money: decimal strings (`"250575.00"`). All dates: `"2026-03-15"`. All datetimes: `"2026-03-15T09:00:00"`

---

## Quick Reference — All 142 Endpoints

### Auth & Users (14)
| Method | Path | Auth | Permission |
|--------|------|------|------------|
| POST | `/v1/auth/login` | Public | — |
| POST | `/v1/auth/refresh` | Public | — |
| POST | `/v1/auth/forgot-password` | Public | — |
| POST | `/v1/auth/reset-password` | Public | — |
| POST | `/v1/auth/logout` | Bearer | any |
| POST | `/v1/auth/change-password` | Bearer | any |
| GET | `/v1/auth/me` | Bearer | any |
| PUT | `/v1/auth/me` | Bearer | any |
| GET | `/v1/users` | Bearer | SYSTEM_USERS |
| GET | `/v1/users/{id}` | Bearer | SYSTEM_USERS |
| POST | `/v1/users` | Bearer | SYSTEM_USERS |
| PUT | `/v1/users/{id}` | Bearer | SYSTEM_USERS |
| DELETE | `/v1/users/{id}` | Bearer | SYSTEM_USERS |
| PUT | `/v1/users/{id}/link-employee` | Bearer | SYSTEM_USERS |

### Employees (21)
| Method | Path | Permission |
|--------|------|------------|
| POST | `/v1/employees` | EMPLOYEE_CREATE |
| GET | `/v1/employees` | EMPLOYEE_VIEW_* (scoped) |
| GET | `/v1/employees/{id}` | EMPLOYEE_VIEW_* (scoped) |
| PUT | `/v1/employees/{id}` | EMPLOYEE_UPDATE |
| PATCH | `/v1/employees/{id}/status` | EMPLOYEE_UPDATE |
| DELETE | `/v1/employees/{id}` | EMPLOYEE_DELETE |
| POST | `/v1/employees/{id}/create-account` | EMPLOYEE_CREATE |
| POST | `/v1/employees/{id}/terminate` | EMPLOYEE_DELETE |
| GET | `/v1/employees/{id}/salary-history` | EMPLOYEE_SALARY_VIEW |
| POST | `/v1/employees/{id}/salary-change` | EMPLOYEE_SALARY_CHANGE |
| GET | `/v1/employees/{id}/documents` | EMPLOYEE_VIEW_* |
| POST | `/v1/employees/{id}/documents` | EMPLOYEE_UPDATE |
| GET | `/v1/employees/{id}/documents/{docId}/download` | EMPLOYEE_VIEW_* |
| DELETE | `/v1/employees/{id}/documents/{docId}` | EMPLOYEE_UPDATE |
| GET | `/v1/employees/{id}/emergency-contacts` | EMPLOYEE_VIEW_* |
| POST | `/v1/employees/{id}/emergency-contacts` | EMPLOYEE_UPDATE |
| PUT | `/v1/employees/{id}/emergency-contacts/{cId}` | EMPLOYEE_UPDATE |
| DELETE | `/v1/employees/{id}/emergency-contacts/{cId}` | EMPLOYEE_UPDATE |
| GET | `/v1/employees/org-chart` | EMPLOYEE_VIEW_TEAM |
| POST | `/v1/employees/import` | EMPLOYEE_CREATE |
| GET | `/v1/employees/export` | EMPLOYEE_VIEW_ALL |

### Departments (5) & Positions (5)
| Method | Path | Permission |
|--------|------|------------|
| POST | `/v1/departments` | EMPLOYEE_CREATE |
| GET | `/v1/departments` | any authenticated |
| GET | `/v1/departments/{id}` | any authenticated |
| PUT | `/v1/departments/{id}` | EMPLOYEE_UPDATE |
| DELETE | `/v1/departments/{id}` | EMPLOYEE_DELETE |
| POST | `/v1/positions` | EMPLOYEE_CREATE |
| GET | `/v1/positions` | any authenticated |
| GET | `/v1/positions/{id}` | any authenticated |
| PUT | `/v1/positions/{id}` | EMPLOYEE_UPDATE |
| DELETE | `/v1/positions/{id}` | EMPLOYEE_DELETE |

### Attendance (20)

**Config-driven:** Frontend reads `GET /v1/settings` → `attendance.check_in_methods` to decide which check-in UI to show. If includes `WEB` → show button on dashboard. If includes `FACE` → show camera button. If `attendance.require_face=true` → face only.

| Method | Path | Permission | Notes |
|--------|------|------------|-------|
| POST | `/v1/attendance/check-in/face` | permitAll (face IS the auth) | Multipart: photo file |
| POST | `/v1/attendance/check-out/face` | permitAll | Multipart: photo file |
| POST | `/v1/attendance/check-in` | ATTENDANCE_CHECKIN | Web/manual: JWT auth, {method?} |
| POST | `/v1/attendance/check-out` | ATTENDANCE_CHECKIN | Web/manual check-out |
| GET | `/v1/attendance/today` | ATTENDANCE_CHECKIN | own status |
| GET | `/v1/attendance/records` | own records | |
| GET | `/v1/attendance/records/employee/{id}` | ATTENDANCE_VIEW_* | |
| GET | `/v1/attendance/records/department/{id}` | ATTENDANCE_VIEW_TEAM | |
| GET | `/v1/attendance/records/daily` | ATTENDANCE_VIEW_ALL | |
| POST | `/v1/attendance/records` | ATTENDANCE_MANAGE | manual entry |
| PUT | `/v1/attendance/records/{id}` | ATTENDANCE_MANAGE | |
| POST | `/v1/attendance/records/bulk-absent` | ATTENDANCE_MANAGE | |
| GET | `/v1/attendance/summary/employee/{id}` | ATTENDANCE_VIEW_* | |
| GET | `/v1/attendance/summary/department/{id}` | ATTENDANCE_VIEW_TEAM | |
| GET | `/v1/attendance/summary/company` | ATTENDANCE_VIEW_ALL | |
| GET | `/v1/attendance/holidays` | any authenticated | |
| POST | `/v1/attendance/holidays` | ATTENDANCE_MANAGE | |
| PUT | `/v1/attendance/holidays/{id}` | ATTENDANCE_MANAGE | |
| DELETE | `/v1/attendance/holidays/{id}` | ATTENDANCE_MANAGE | |
| GET | `/v1/attendance/schedules` | any authenticated | |

### Leave (19)
| Method | Path | Permission |
|--------|------|------------|
| GET | `/v1/leave/types` | any authenticated |
| POST | `/v1/leave/types` | LEAVE_BALANCE_MANAGE |
| PUT | `/v1/leave/types/{id}` | LEAVE_BALANCE_MANAGE |
| DELETE | `/v1/leave/types/{id}` | LEAVE_BALANCE_MANAGE |
| POST | `/v1/leave/requests` | LEAVE_REQUEST_OWN |
| GET | `/v1/leave/requests` | own requests |
| GET | `/v1/leave/requests/{id}` | own or LEAVE_APPROVE_* |
| PUT | `/v1/leave/requests/{id}/approve` | LEAVE_APPROVE_* |
| PUT | `/v1/leave/requests/{id}/reject` | LEAVE_APPROVE_* |
| PUT | `/v1/leave/requests/{id}/cancel` | own or LEAVE_APPROVE_ALL |
| GET | `/v1/leave/requests/pending` | LEAVE_APPROVE_* |
| GET | `/v1/leave/requests/team` | LEAVE_APPROVE_TEAM |
| GET | `/v1/leave/requests/all` | LEAVE_APPROVE_ALL |
| GET | `/v1/leave/balances` | own |
| GET | `/v1/leave/balances/employee/{id}` | LEAVE_BALANCE_MANAGE |
| GET | `/v1/leave/balances/department/{id}` | LEAVE_APPROVE_TEAM |
| POST | `/v1/leave/balances/initialize` | LEAVE_BALANCE_MANAGE |
| PUT | `/v1/leave/balances/{id}/adjust` | LEAVE_BALANCE_MANAGE |
| GET | `/v1/leave/calendar` | LEAVE_APPROVE_TEAM |

### Payroll (23)
| Method | Path | Permission |
|--------|------|------------|
| POST | `/v1/payroll/periods` | PAYROLL_PROCESS |
| GET | `/v1/payroll/periods` | PAYROLL_VIEW |
| GET | `/v1/payroll/periods/{id}` | PAYROLL_VIEW |
| POST | `/v1/payroll/periods/{id}/generate` | PAYROLL_PROCESS |
| POST | `/v1/payroll/periods/{id}/approve` | PAYROLL_APPROVE |
| POST | `/v1/payroll/periods/{id}/mark-paid` | PAYROLL_PAY |
| POST | `/v1/payroll/periods/{id}/lock` | SYSTEM_SETTINGS |
| GET | `/v1/payroll/jobs/{jobId}/status` | PAYROLL_VIEW |
| GET | `/v1/payroll/periods/{id}/payslips` | PAYROLL_VIEW |
| GET | `/v1/payroll/payslips/{id}` | PAYROLL_VIEW |
| PATCH | `/v1/payroll/payslips/{id}/adjust` | PAYSLIP_ADJUST |
| POST | `/v1/payroll/payslips/{id}/recalculate` | PAYSLIP_ADJUST |
| GET | `/v1/payroll/payslips/{id}/pdf` | PAYROLL_VIEW |
| POST | `/v1/payroll/payslips/{id}/approve-flagged` | PAYROLL_APPROVE |
| GET | `/v1/payroll/my-payslips` | PAYSLIP_VIEW_OWN |
| GET | `/v1/payroll/my-payslips/period/{id}` | PAYSLIP_VIEW_OWN |
| GET | `/v1/payroll/my-payslips/{id}/pdf` | PAYSLIP_VIEW_OWN |
| GET | `/v1/payroll/ytd/employee/{id}` | PAYROLL_VIEW |
| GET | `/v1/payroll/additions` | PAYROLL_VIEW |
| POST | `/v1/payroll/additions` | PAYSLIP_ADJUST |
| PUT | `/v1/payroll/additions/{id}` | PAYSLIP_ADJUST |
| DELETE | `/v1/payroll/additions/{id}` | PAYSLIP_ADJUST |
| POST | `/v1/payroll/additions/bulk` | PAYSLIP_ADJUST |

### AI/ML (8)
| Method | Path | Permission |
|--------|------|------------|
| POST | `/v1/ai/payroll/detect` | (internal — payroll-service only) |
| POST | `/v1/ai/payroll/detect/batch` | (internal) |
| POST | `/v1/ai/attendance/fraud-detect` | (internal) |
| GET | `/v1/ai/attrition/risk` | AI_DASHBOARD |
| GET | `/v1/ai/attrition/risk/employee/{id}` | AI_DASHBOARD |
| GET | `/v1/ai/attrition/dashboard` | AI_DASHBOARD |
| GET | `/v1/ai/payroll/forecast` | AI_DASHBOARD |
| GET | `/v1/ai/health` | SYSTEM_SETTINGS |

### Reports (12)
| Method | Path | Permission |
|--------|------|------------|
| GET | `/v1/reports/payroll-summary` | REPORT_PAYROLL |
| GET | `/v1/reports/payroll-summary/pdf` | REPORT_PAYROLL |
| GET | `/v1/reports/form200` | REPORT_FORM200 |
| GET | `/v1/reports/salary-breakdown` | REPORT_PAYROLL |
| GET | `/v1/reports/attendance-monthly` | REPORT_ATTENDANCE |
| GET | `/v1/reports/attendance-summary` | REPORT_ATTENDANCE |
| GET | `/v1/reports/leave-balances` | REPORT_LEAVE |
| GET | `/v1/reports/employee-directory` | REPORT_PAYROLL |
| GET | `/v1/reports/turnover` | REPORT_EXECUTIVE |
| GET | `/v1/reports/headcount` | REPORT_EXECUTIVE |
| GET | `/v1/reports/executive-summary` | REPORT_EXECUTIVE |
| GET | `/v1/reports/ai-insights` | AI_DASHBOARD |

### Notifications (5)
| Method | Path | Permission |
|--------|------|------------|
| GET | `/v1/notifications` | own |
| GET | `/v1/notifications/unread-count` | own |
| PUT | `/v1/notifications/{id}/read` | own |
| PUT | `/v1/notifications/read-all` | own |
| DELETE | `/v1/notifications/{id}` | own |

### Integration & Settings (7)
| Method | Path | Permission |
|--------|------|------------|
| POST | `/v1/integration/sync/{periodId}` | SYSTEM_SETTINGS |
| GET | `/v1/integration/sync/status/{jobId}` | SYSTEM_SETTINGS |
| GET | `/v1/integration/sync/history` | SYSTEM_SETTINGS |
| POST | `/v1/integration/retry/{jobId}` | SYSTEM_SETTINGS |
| GET | `/v1/integration/bank-file/{periodId}` | PAYROLL_PAY |
| GET | `/v1/settings` | SYSTEM_SETTINGS |
| PUT | `/v1/settings/{key}` | SYSTEM_SETTINGS |

### Dashboard (1)
| Method | Path | Permission |
|--------|------|------------|
| GET | `/v1/dashboard/stats` | any authenticated |

---

## Detailed Request/Response Schemas

### Auth

**POST `/v1/auth/login`** — Public
```json
// Request
{ "email": "admin@hrms.kz", "password": "password123" }
// Response 200
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "email": "admin@hrms.kz",
    "firstName": "Nursultan",
    "lastName": "Admin",
    "role": "SUPER_ADMIN",
    "permissions": ["EMPLOYEE_CREATE", "PAYROLL_PROCESS", ...],
    "employeeId": "uuid or null"
  }
}
```

**GET `/v1/auth/me`**
```json
// Response 200
{
  "id": "uuid",
  "email": "user@hrms.kz",
  "firstName": "Иван",
  "lastName": "Иванов",
  "role": "EMPLOYEE",
  "permissions": ["EMPLOYEE_VIEW_OWN", "PAYSLIP_VIEW_OWN", "LEAVE_REQUEST_OWN", "ATTENDANCE_CHECKIN"],
  "employee": {
    "id": "uuid",
    "employeeNumber": "EMP-202601-001",
    "fullName": "Иванов Иван Иванович",
    "department": { "id": "uuid", "name": "Engineering" },
    "position": { "id": "uuid", "title": "Senior Developer" },
    "baseSalary": "300000.00",
    "hireDate": "2024-01-15",
    "status": "ACTIVE"
  }
}
```

### Employee

**POST `/v1/employees`**
```json
// Request
{
  "firstName": "Иван", "lastName": "Иванов", "middleName": "Петрович",
  "email": "ivan@company.kz", "iin": "123456789012",
  "phone": "+77001234567", "hireDate": "2026-04-01",
  "dateOfBirth": "1995-05-20",
  "employmentType": "FULL_TIME",
  "baseSalary": "300000.00",
  "departmentId": "uuid", "positionId": "uuid", "managerId": "uuid",
  "bankAccount": "KZ12345678901234", "bankName": "Kaspi Bank",
  "isResident": true, "hasDisability": false, "isPensioner": false,
  "createAccount": true  // optionally create user account
}
// Response 201
{
  "id": "uuid", "employeeNumber": "EMP-202604-042",
  "firstName": "Иван", "lastName": "Иванов", "middleName": "Петрович",
  "fullName": "Иванов Иван Петрович",
  "email": "ivan@company.kz", "iin": "123456789012",
  "hireDate": "2026-04-01", "status": "ACTIVE",
  "employmentType": "FULL_TIME",
  "baseSalary": "300000.00",
  "department": { "id": "uuid", "name": "Engineering" },
  "position": { "id": "uuid", "title": "Developer" },
  "manager": { "id": "uuid", "fullName": "Петров Петр" }
}
```

**GET `/v1/employees`** — Paginated
```
?search=иванов&departmentId=uuid&status=ACTIVE&type=FULL_TIME&page=0&size=20&sort=lastName,asc
```
```json
// Response 200 (Page<EmployeeSummary>)
{
  "content": [
    {
      "id": "uuid", "employeeNumber": "EMP-202601-001",
      "fullName": "Иванов Иван Петрович", "email": "ivan@company.kz",
      "department": "Engineering", "position": "Senior Developer",
      "status": "ACTIVE", "hireDate": "2024-01-15"
    }
  ],
  "totalElements": 150, "totalPages": 8, "number": 0, "size": 20
}
```

### Leave

**POST `/v1/leave/requests`**
```json
// Request
{ "leaveTypeId": "uuid", "startDate": "2026-06-01", "endDate": "2026-06-06", "reason": "Отпуск" }
// Response 201
{
  "id": "uuid",
  "employee": { "id": "uuid", "fullName": "Иванов Иван" },
  "leaveType": { "id": "uuid", "name": "Annual Leave", "isPaid": true },
  "startDate": "2026-06-01", "endDate": "2026-06-06", "daysRequested": 6,
  "reason": "Отпуск", "status": "PENDING", "createdAt": "2026-05-15T10:30:00"
}
```

**GET `/v1/leave/balances`** — Own
```json
// Response 200
[
  {
    "id": "uuid",
    "leaveType": { "id": "uuid", "name": "Annual Leave" },
    "year": 2026, "entitledDays": 24, "carriedOver": 3,
    "usedDays": 6, "adjustedDays": 0, "remainingDays": 21
  },
  {
    "leaveType": { "name": "Sick Leave" },
    "entitledDays": 30, "usedDays": 2, "remainingDays": 28
  }
]
```

### Attendance

**Config check (do this on page load):**
```typescript
// Fetch settings to determine which check-in UI to show
const settings = await api.get('/v1/settings');
const methods = settings.data['attendance.check_in_methods']; // "FACE,WEB,MANUAL"
const requireFace = settings.data['attendance.require_face'] === 'true';

const showWebButton = methods.includes('WEB') && !requireFace;
const showFaceButton = methods.includes('FACE');
```

**POST `/v1/attendance/check-in/face`** — Face recognition (no JWT needed, face IS the auth)
```typescript
// Request: multipart/form-data with photo file
const formData = new FormData();
formData.append('photo', capturedImageBlob, 'face.jpg');
const response = await fetch('/api/v1/attendance/check-in/face', {
  method: 'POST',
  body: formData   // NO Authorization header — kiosk mode
});
```
```json
// Response 200 — face matched, checked in
{
  "id": "uuid", "workDate": "2026-04-08",
  "checkIn": "2026-04-08T09:05:00", "checkOut": null,
  "status": "PRESENT", "workedHours": null,
  "method": "FACE",
  "employeeName": "Иванов Иван",
  "faceConfidence": 0.94
}
// Response 401 — face not recognized
{ "success": false, "message": "Face not recognized: no_match" }
// Response 503 — AI service down
{ "success": false, "message": "Face recognition unavailable. Use manual check-in." }
```

**POST `/v1/attendance/check-in`** — Web/manual (requires JWT)
```json
// Request (all optional — defaults from JWT)
{ "method": "WEB", "locationLat": 51.128, "locationLng": 71.430 }
// Response 200
{
  "id": "uuid", "workDate": "2026-04-08",
  "checkIn": "2026-04-08T09:05:00", "checkOut": null,
  "status": "PRESENT", "workedHours": null,
  "method": "WEB"
}
// Error 400 if already checked in
// Error 403 if method not allowed by config
{ "success": false, "message": "Check-in method 'WEB' is not enabled. Allowed: FACE" }
```

**GET `/v1/attendance/today`** — My status
```json
// Response 200 (checked in)
{
  "checkedIn": true,
  "checkInTime": "2026-04-08T09:05:00",
  "checkedOut": false,
  "status": "PRESENT",
  "method": "FACE",
  "workedHours": null
}
// Response 200 (not checked in)
{ "checkedIn": false }
```

**GET `/v1/attendance/summary/employee/{id}?year=2026&month=3`**
```json
{
  "employeeId": "uuid", "year": 2026, "month": 3,
  "presentDays": 18, "lateDays": 2, "absentDays": 1, "halfDays": 0,
  "holidayDays": 1, "totalWorkedHours": "152.50", "overtimeHours": "4.25"
}
```

### Payroll

**GET `/v1/payroll/payslips/{id}`** — Full detail
```json
{
  "id": "uuid",
  "period": { "id": "uuid", "year": 2026, "month": 3, "name": "Март 2026" },
  "employee": { "id": "uuid", "fullName": "Иванов Иван", "iin": "123456789012" },
  "workedDays": 22, "totalWorkingDays": 22,
  "grossSalary": "300000.00", "earnedSalary": "300000.00",
  "allowances": "0.00", "otherDeductions": "0.00",
  "opvAmount": "30000.00", "vosmsAmount": "6000.00",
  "taxableIncome": "134250.00", "ipnAmount": "13425.00",
  "totalDeductions": "49425.00", "netSalary": "250575.00",
  "soAmount": "13500.00", "snAmount": "18000.00", "opvrAmount": "10500.00",
  "mrpUsed": 4325, "isResident": true,
  "status": "DRAFT",
  "anomalyScore": null, "anomalyFlags": null, "aiReviewed": false
}
```

### Notifications

**GET `/v1/notifications?page=0&size=20`**
```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Leave Approved",
      "message": "Your leave request for 6 days has been approved",
      "type": "LEAVE_APPROVED",
      "isRead": false,
      "referenceType": "LEAVE_REQUEST",
      "referenceId": "uuid",
      "createdAt": "2026-04-08T14:30:00"
    }
  ],
  "totalElements": 45
}
```

**GET `/v1/notifications/unread-count`**
```json
{ "count": 3 }
```

### AI

**GET `/v1/ai/attrition/risk?departmentId=uuid`**
```json
[
  {
    "employeeId": "uuid", "fullName": "Иванов Иван",
    "attritionRisk": 0.72, "riskLevel": "HIGH",
    "topFactors": [
      { "factor": "salary_below_average", "impact": 0.35, "detail": "15% below position avg" },
      { "factor": "no_promotion_18_months", "impact": 0.25 }
    ],
    "recommendedActions": ["Schedule retention conversation", "Review salary"]
  }
]
```

### Reports — Download Pattern

All report endpoints return binary files. Frontend must handle as blob:
```typescript
const response = await api.get('/v1/reports/payroll-summary', {
  params: { periodId },
  responseType: 'blob'
});
const url = window.URL.createObjectURL(new Blob([response.data]));
const link = document.createElement('a');
link.href = url;
link.setAttribute('download', 'payroll_summary.xlsx');
link.click();
```

---

## Error Response Format

```json
// 400 Validation Error
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Invalid email format",
    "baseSalary": "Salary must be positive"
  },
  "timestamp": "2026-04-08T10:00:00"
}

// 401 Unauthorized
{ "success": false, "message": "Invalid or expired token" }

// 403 Forbidden
{ "success": false, "message": "Insufficient permissions" }

// 404 Not Found
{ "success": false, "message": "Employee not found with id: uuid" }

// 409 Conflict
{ "success": false, "message": "Email already registered" }
```

## Formatting Notes for Nurbol

- Dates display: `dd.MM.yyyy` (Kazakhstan standard: `08.04.2026`)
- Money display: `₸ 300,000.00` — use `Intl.NumberFormat('ru-KZ', {style:'currency', currency:'KZT'})`
- Status badge colors: ACTIVE/APPROVED/PRESENT=green, PENDING/PROCESSING=yellow, REJECTED/ABSENT/FLAGGED=red, DRAFT=gray, PAID=blue, LOCKED=purple
- Pagination: backend uses 0-indexed pages. UI shows 1-indexed.
