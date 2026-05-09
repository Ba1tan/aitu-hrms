# User Service

**Port:** 8081 | **Schema:** hrms_user | **Owner:** Nursultan

## Responsibility
Authentication, authorization, user CRUD, permission-based RBAC, audit logging, session management.

## Tech
Spring Boot 3.3.5, Java 17, PostgreSQL 16, Redis 7 (JWT blacklist + rate limiting)

## Tables (in hrms_user schema)
- `users` — accounts with role, login tracking, lockout fields
- `permissions` — 35 permission codes (EMPLOYEE_CREATE, PAYROLL_PROCESS, EMPLOYEE_BIOMETRIC, etc. — V4 added EMPLOYEE_VIEW_OWN/ALL + EMPLOYEE_BIOMETRIC)
- `role_permissions` — role→permission mapping (8 roles × N permissions)
- `audit_logs` — every sensitive action with JSONB old/new values

## Endpoints (14)

```
# Public (no JWT)
POST /v1/auth/login                    {email, password} → {accessToken, refreshToken, user}
POST /v1/auth/refresh                  {refreshToken} → new tokens
POST /v1/auth/forgot-password          {email} → sends reset email
POST /v1/auth/reset-password           {token, newPassword}

# Authenticated
POST /v1/auth/logout                   Blacklist current token in Redis
POST /v1/auth/change-password          {currentPassword, newPassword}
GET  /v1/auth/me                       Current user profile + linked employee data
PUT  /v1/auth/me                       Update own name, phone

# User Management (SYSTEM_USERS permission)
GET  /v1/users                         List all (paginated, ?search=&role=)
GET  /v1/users/{id}                    Detail
POST /v1/users                         Create {email, password, role, employeeId?}
PUT  /v1/users/{id}                    Update {role, enabled, locked}
DELETE /v1/users/{id}                  Soft delete
PUT  /v1/users/{id}/link-employee      {employeeId} — link user to employee
```

## Auth Flow
1. Login → validate credentials → generate access (15min) + refresh (7d) tokens
2. Every request → JwtAuthenticationFilter extracts token → validates → loads User with permissions
3. Logout → add token to Redis blacklist (TTL = token remaining life)
4. Refresh → validate refresh token, issue new pair, blacklist old access token

## Login Security
- After 5 failed attempts → lock account for 30 minutes (`locked_until`)
- Track `failed_login_count`, reset on successful login
- Update `last_login_at` and `last_login_ip` on success

## Permission Loading
```java
// User.getAuthorities() returns both role and permissions:
// ROLE_HR_MANAGER + PAYROLL_PROCESS + PAYROLL_APPROVE + EMPLOYEE_CREATE + ...
// Permissions loaded from role_permissions table, cached in Redis (1h TTL)
```

## Audit Log
Every write operation on sensitive entities calls:
```java
auditService.log(userId, "APPROVE", "PAYSLIP", payslipId, oldState, newState, ip, userAgent);
```

## Cross-Service Communication
- **Consumes:** EmployeeCreatedEvent → auto-create user account
- **Publishes:** UserAccountCreatedEvent, PasswordResetRequestedEvent
- **Feign clients used by other services:** None (other services validate JWT locally using shared secret)

## Environment Variables
```
DB_URL=jdbc:postgresql://postgres:5432/hrms?currentSchema=hrms_user
DB_USERNAME=hrms_user
DB_PASSWORD=
REDIS_HOST=redis
JWT_SECRET=
JWT_ACCESS_EXPIRY=900000
JWT_REFRESH_EXPIRY=604800000
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=
MAIL_PASSWORD=
```
