# Reporting Service.md

**Port:** 8087 | **Schema:** hrms_reporting (materialized views only) | **Owner:** Nursultan

## Responsibility
Generate downloadable XLSX/PDF reports. No business logic — reads data from other services via Feign, formats into documents. Apache POI for XLSX, OpenPDF for PDF.

## Endpoints (12)

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

# AI (AI_DASHBOARD)
GET /v1/reports/ai-insights             → PDF (anomalies, attrition risks, forecasts)
```

## Response Pattern
```java
response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
response.setHeader("Content-Disposition", "attachment; filename=report.xlsx");
reportService.generate(params, response.getOutputStream());
```

## Feign Clients
- `payroll-service` → payslip data for payroll reports
- `employee-service` → employee list for directory/headcount
- `attendance-service` → attendance records for monthly grid
- `leave-service` → leave balances
- `ai-ml-service` → attrition risks for AI report

## Events Consumed
- `PayrollJobCompletedEvent` → optionally pre-generate payroll summary report
