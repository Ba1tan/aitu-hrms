# Notification Service.md

**Port:** 8088 | **Schema:** hrms_notification | **Owner:** Askar

## Responsibility
Store in-app notifications, send emails (SMTP), push notifications (FCM for mobile). Consumes events from all other services via RabbitMQ. NEVER throws exceptions — failures are logged and swallowed.

## Tables
- `notifications` — per-user records with type, channel (IN_APP/EMAIL/SMS/PUSH), is_read, reference_type/id

## Endpoints (5)

```
GET    /v1/notifications                My notifications (?page=&size=, newest first)
GET    /v1/notifications/unread-count   Integer count (for bell badge in UI)
PUT    /v1/notifications/{id}/read      Mark one as read (sets read_at)
PUT    /v1/notifications/read-all       Mark all as read
DELETE /v1/notifications/{id}           Soft delete
```

## Events Consumed (ALL via RabbitMQ)

| Event | Notification Created |
|-------|---------------------|
| `EmployeeCreatedEvent` | → HR: "New employee {name} onboarded" |
| `EmployeeTerminatedEvent` | → HR: "Employee {name} terminated" |
| `LeaveRequestCreatedEvent` | → Manager: "{name} requested {days} days leave" |
| `LeaveApprovedEvent` | → Employee: "Your leave request approved" |
| `LeaveRejectedEvent` | → Employee: "Your leave request rejected: {comment}" |
| `PayrollJobStartedEvent` | → HR: "Payroll processing started for {month}" |
| `PayrollJobCompletedEvent` | → HR: "Payroll completed! {count} employees, ₸{total}" |
| `PayrollAnomalyDetectedEvent` | → HR: "⚠️ Flagged payslip for {name} — score {score}" |
| `FraudAttemptDetectedEvent` | → HR: "🚨 Fraud alert: {name}, score {score}" |
| `UserAccountCreatedEvent` | → Employee: "Welcome! Your account is ready" (email with temp password) |
| `PasswordResetRequestedEvent` | → User: password reset email with token link |
| `IntegrationSyncFailedEvent` | → HR: "1C sync failed for {period}" |

## CRITICAL: Non-Throwing Pattern
```java
@RabbitListener(queues = "notification.leave.approved")
public void onLeaveApproved(LeaveApprovedEvent event) {
    try {
        Notification n = Notification.builder()
            .userId(resolveUserId(event.getEmployeeId()))
            .title("Leave Approved")
            .message("Your leave from " + event.getStartDate() + " to " + event.getEndDate() + " approved")
            .type(NotificationType.LEAVE_APPROVED)
            .referenceType("LEAVE_REQUEST")
            .referenceId(event.getRequestId())
            .build();
        notificationRepo.save(n);
        
        if (emailEnabled) {
            emailService.send(resolveEmail(event.getEmployeeId()), "Leave Approved", ...);
        }
    } catch (Exception e) {
        log.error("Failed to process LeaveApprovedEvent: {}", e.getMessage());
        // SWALLOW — never propagate, never block the publishing service
    }
}
```

## Email Templates (Thymeleaf)
```
src/main/resources/templates/email/
├── welcome.html           # New employee with credentials
├── password-reset.html    # Reset link
├── leave-approved.html
├── leave-rejected.html
├── payslip-ready.html
├── fraud-alert.html       # For HR
└── payroll-anomaly.html   # For HR
```

## Feign Clients
- `employee-service` → resolve employeeId → email address
- `user-service` → resolve employeeId → userId for in-app notification
