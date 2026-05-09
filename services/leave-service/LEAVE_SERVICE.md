# Leave Service

**Port:** 8084 | **Schema:** hrms_leave | **Owner:** Askar

## Responsibility
Leave types configuration, leave request submission/approval/rejection/cancellation, balance management, carryover, leave calendar.

## Tables
- `leave_types` — Annual(24d), Sick(30d), Maternity(126d), Unpaid(14d), Study(10d) + custom. Fields: days_allowed, is_paid, requires_approval, carryover_allowed, carryover_max_days
- `leave_requests` — employee requests with status flow: PENDING→APPROVED/REJECTED/CANCELLED
- `leave_balances` — per employee per type per year. `remaining_days` is GENERATED ALWAYS AS (entitled_days + carried_over + adjusted_days - used_days) — DO NOT SET directly
- `balance_adjustments` — audit trail for manual balance changes

## Endpoints (19)

```
# Leave Types (LEAVE_BALANCE_MANAGE)
GET    /v1/leave/types                              List all
POST   /v1/leave/types                              {name, daysAllowed, isPaid, requiresApproval, carryoverAllowed}
PUT    /v1/leave/types/{id}
DELETE /v1/leave/types/{id}

# Leave Requests
POST   /v1/leave/requests                           {leaveTypeId, startDate, endDate, reason}
GET    /v1/leave/requests                           Own requests (?status=&page=&size=)
GET    /v1/leave/requests/{id}                      Detail
PUT    /v1/leave/requests/{id}/approve              LEAVE_APPROVE_TEAM or LEAVE_APPROVE_ALL
PUT    /v1/leave/requests/{id}/reject               {comment}
PUT    /v1/leave/requests/{id}/cancel               Cancel own (PENDING) or reverse balance (APPROVED)
GET    /v1/leave/requests/pending                   Pending for my approval
GET    /v1/leave/requests/team                      My team's requests (?status=)
GET    /v1/leave/requests/all                       All (LEAVE_APPROVE_ALL) (?status=&departmentId=&page=)

# Leave Balances
GET    /v1/leave/balances                           Own balances (current year)
GET    /v1/leave/balances/employee/{id}             ?year=
GET    /v1/leave/balances/department/{id}           Summary
POST   /v1/leave/balances/initialize                {year} → creates for all active employees
PUT    /v1/leave/balances/{id}/adjust               {days, reason} → inserts balance_adjustment
POST   /v1/leave/balances/carryover                 {fromYear} → rolls unused annual leave

# Calendar
GET    /v1/leave/calendar                           ?month=&year=&departmentId= → [{employee, dates, type}]
```

## Business Rules

1. **Balance check:** remaining_days >= days_requested, else 400
2. **Overlap check:** no approved requests overlapping same dates for same employee
3. **Approval scope:** LEAVE_APPROVE_TEAM → only direct reports. LEAVE_APPROVE_ALL → anyone. Cannot approve own request.
4. **Approve transaction:** atomic update of leave_requests.status + leave_balances.used_days
5. **Cancel APPROVED:** reverse used_days in same transaction. Only HR can cancel approved requests.
6. **Cancel PENDING:** employee can cancel their own pending requests
7. **Carryover:** max 50% of annual leave (configurable via company_settings). Creates carried_over in next year's balance.

## Approval Transaction
```java
@Transactional
public LeaveRequestResponse approve(UUID requestId, UUID reviewerId) {
    LeaveRequest req = findOrThrow(requestId); // validates PENDING
    validateApprovalAuthority(reviewerId, req.getEmployeeId()); // checks manager relationship
    
    req.setStatus(APPROVED);
    req.setReviewedBy(reviewerId);
    req.setReviewedAt(LocalDateTime.now());
    requestRepo.save(req);
    
    LeaveBalance balance = balanceRepo.findByEmployeeIdAndLeaveTypeIdAndYear(...)
        .orElseThrow(() -> new BusinessException("No balance found"));
    balance.setUsedDays(balance.getUsedDays() + req.getDaysRequested());
    balanceRepo.save(balance);
    
    // Non-critical notification
    notificationService.notify(req.getEmployeeId(), "Leave Approved", ...);
    
    // Publish event for attendance-service to mark ON_LEAVE
    rabbitTemplate.convertAndSend("hrms.events", "leave.approved",
        LeaveApprovedEvent.builder().employeeId(...).startDate(...).endDate(...).build());
    
    return mapToResponse(req);
}
```

## Events Published
- `LeaveRequestCreatedEvent` {requestId, employeeId, managerId, type, dates}
- `LeaveApprovedEvent` {requestId, employeeId, startDate, endDate, leaveType}
- `LeaveRejectedEvent` {requestId, employeeId, comment}

## Events Consumed
- `EmployeeCreatedEvent` → auto-initialize leave balances for current year

## Feign Clients
- `employee-service` → get employee's manager_id for approval validation
