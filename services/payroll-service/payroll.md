# Payroll Service — CLAUDE.md

**Port:** 8085 | **Schema:** hrms_payroll | **Owner:** Nursultan

## Responsibility
Kazakhstan 2026 Tax Code payroll calculation, payslip generation (sync + Spring Batch), AI anomaly detection integration, payslip PDF generation, payroll additions (bonuses/deductions), salary advances, year-to-date tracking.

## Tables
- `payroll_periods` — monthly periods with status: DRAFT→PROCESSING→COMPLETED→APPROVED→PAID→LOCKED
- `payslips` — per-employee per-period with all 10 tax calculation fields + anomaly_score/flags + status (DRAFT/FLAGGED/APPROVED/PAID)
- `payroll_additions` — bonuses (MEAL_ALLOWANCE, OVERTIME, BONUS_PERFORMANCE...) and deductions (FINE, ADVANCE_REPAYMENT...) per employee per period
- `salary_advances` — advance tracking with installment repayment

## Kazakhstan 2026 Tax Code
```
МРП=4325 МЗП=85000
1. earned = gross × (worked/total)
2. OPV = earned×10% (cap 50×МЗП=4,250,000, skip if pensioner)
3. ВОСМС = earned×2% (cap 20×МЗП=1,700,000)
4. deduction = 30×МРП=129,750 (residents) +882×МРП(disab grp3) +5000×МРП(disab grp1/2)
5. taxable = earned−OPV−ВОСМС−deduction (floor 0)
6. IPN = taxable×10% (resident) or 20% (non-resident)
7. net = earned−OPV−ВОСМС−IPN+allowances−deductions
Employer: SO=(earned−OPV)×5%  SN=earned×6%(fixed)  ОПВР=earned×3.5%
```
All money: BigDecimal. NEVER double/float.

## Endpoints (23)

```
# Periods
POST /v1/payroll/periods                          {year, month, workingDays}
GET  /v1/payroll/periods                          Paginated list
GET  /v1/payroll/periods/{id}                     Detail with summary stats
POST /v1/payroll/periods/{id}/generate            Generate payslips (calls AI for each)
POST /v1/payroll/periods/{id}/approve             Approve (PAYROLL_APPROVE)
POST /v1/payroll/periods/{id}/mark-paid           Mark paid (PAYROLL_PAY)
POST /v1/payroll/periods/{id}/lock                Lock (SUPER_ADMIN)

# Batch Job Status
GET  /v1/payroll/jobs/{jobId}/status              Check Spring Batch job progress

# Payslips
GET  /v1/payroll/periods/{id}/payslips            All for period (?status=&search=)
GET  /v1/payroll/payslips/{id}                    Detail (all tax breakdown fields)
PATCH /v1/payroll/payslips/{id}/adjust            {allowances?, deductions?, workedDays?}
POST /v1/payroll/payslips/{id}/recalculate        Recalculate after adjustment
GET  /v1/payroll/payslips/{id}/pdf                Download PDF
POST /v1/payroll/payslips/{id}/approve-flagged    Approve AI-flagged payslip after manual review

# Employee Self-Service
GET  /v1/payroll/my-payslips                      Own payslips (PAYSLIP_VIEW_OWN)
GET  /v1/payroll/my-payslips/period/{id}          Own payslip for period
GET  /v1/payroll/my-payslips/{id}/pdf             Download own PDF

# Year-to-Date
GET  /v1/payroll/ytd/employee/{id}                ?year= → cumulative {totalOpv, totalIpn, totalVosms...}

# Additions
GET  /v1/payroll/additions                        ?periodId=&employeeId=
POST /v1/payroll/additions                        {employeeId, periodId, type, category, amount, isTaxable}
PUT  /v1/payroll/additions/{id}
DELETE /v1/payroll/additions/{id}
POST /v1/payroll/additions/bulk                   {periodId, employeeIds[], type, category, amount}
```

## Payslip Generation Flow
```
1. Validate period is DRAFT
2. Set period status = PROCESSING
3. Fetch all active employees (or filtered list)
4. For each employee:
   a. Get worked days from attendance-service (Feign)
   b. Get approved unpaid leave days from leave-service (Feign)
   c. Get additions for this period (bonuses/deductions)
   d. Calculate: KazakhstanPayrollCalculator.calculate(employee, workedDays, totalDays, allowances, deductions)
   e. Call AI service: POST /v1/ai/payroll/detect → get anomaly_score
   f. If anomaly_score > 0.65 → payslip.status = FLAGGED + save flags
   g. Else → payslip.status = DRAFT
   h. Insert payslip
5. Set period status = COMPLETED
6. Publish PayrollJobCompletedEvent
```

## Events Published
- `PayrollJobStartedEvent` {periodId, employeeCount}
- `PayrollJobCompletedEvent` {periodId, totalGross, totalNet, employeeCount}
- `PayrollAnomalyDetectedEvent` {payslipId, employeeId, anomalyScore, flags}
- `PayrollPeriodApprovedEvent` {periodId} → triggers 1C sync

## Events Consumed
- `EmployeeCreatedEvent` → initialize payroll settings for new employee
- `SalaryChangedEvent` → log for next payroll calculation

## Feign Clients
- `employee-service` → get active employees, salary, dept info
- `attendance-service` → get worked days for period
- `leave-service` → get approved unpaid leave days for period
- `ai-ml-service` → POST /v1/ai/payroll/detect (non-critical try/catch)
