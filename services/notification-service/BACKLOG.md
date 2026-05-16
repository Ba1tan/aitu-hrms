# notification-service — v1 Backlog

Items deferred from v1 scope per CLAUDE.md §14.

## Infrastructure

- **DLX / retry exchange** — add dead-letter exchange with exponential backoff for failed messages (EVENTS.md §6).
- **WebSocket / SSE** — real-time in-app notification push to connected clients.

## Push notifications (FCM)

- **Real FCM implementation** — replace `FcmPushSender` stub with `firebase-admin` SDK. Add `com.google.firebase:firebase-admin:9.x` dependency.
- **Device-token storage** — store per-user FCM device tokens in DB or Redis.

## Missing events

- **`IntegrationSyncFailedEvent` listener** — payload not defined in hrms-common or EVENTS.md. Skip until integration-hub specifies the schema.

## User preferences

- **Per-user notification preferences** — allow users to opt out of specific `NotificationType` / `NotificationChannel` combinations. Requires a `notification_preferences` table and a settings endpoint.

## SMS

- **SMS sending** — `NotificationChannel.SMS` currently throws `UnsupportedOperationException`. Implement via a Kazakh SMS gateway (e.g., SMSC.kz or KAZTELECOM API).

## Email

- **Email delivery tracking** — publish `notification.sent` event after successful SMTP delivery for audit purposes (event type reserved).

## Performance

- **Fan-out optimization for `PayrollPeriodApprovedEvent`** — current O(n) synchronous loop is slow for large employee counts (5000+). v2 should use `@Async` or split into a dedicated fan-out queue.

## Pending dependencies on other services (see CLAUDE.md §15)

- `GET /v1/users/by-employee/{employeeId}` — not yet implemented in user-service.
- `GET /v1/users/by-permission/{permissionCode}` — not yet implemented in user-service.
- `X-Service-Token` header acceptance — user-service, employee-service, payroll-service must validate this header for service-to-service calls.
- 5 event producers (leave-service, attendance-service, user-service) must emit JSON matching CLAUDE.md §4 DTOs.
