package kz.aitu.hrms.notification.service;

import kz.aitu.hrms.common.event.*;
import kz.aitu.hrms.notification.client.dto.PayslipBriefDto;
import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationChannel;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.event.dto.*;
import kz.aitu.hrms.notification.service.email.EmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationFactory {

    private final RecipientResolver recipientResolver;

    @Value("${notification.mail.base-url:https://hrms.nursnerv.uk}")
    private String baseUrl;

    @Value("${notification.mail.from:no-reply@hrms.kz}")
    private String mailFrom;

    public record BuiltNotification(Notification notification, String idempotencyKey, EmailRequest emailRequest) {}

    // ── Employee events ──────────────────────────────────────────────────────

    public BuiltNotification fromEmployeeCreated(EmployeeCreatedEvent e, UUID recipientUserId) {
        String title = "Новый сотрудник";
        String message = "Зарегистрирован новый сотрудник: " + e.getFullName();
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.EMPLOYEE_ONBOARDED)
                .channel(NotificationChannel.IN_APP)
                .referenceType("EMPLOYEE")
                .referenceId(e.getEmployeeId())
                .build();
        String key = "EMPLOYEE_ONBOARDED:" + e.getEmployeeId() + ":" + recipientUserId;
        return new BuiltNotification(n, key, null);
    }

    public BuiltNotification fromEmployeeTerminated(EmployeeTerminatedEvent e, UUID recipientUserId) {
        String title = "Сотрудник уволен";
        String message = "Сотрудник уволен с " + e.getTerminationDate();
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.EMPLOYEE_TERMINATED)
                .channel(NotificationChannel.IN_APP)
                .referenceType("EMPLOYEE")
                .referenceId(e.getEmployeeId())
                .build();
        String key = "EMPLOYEE_TERMINATED:" + e.getEmployeeId() + ":" + recipientUserId;
        return new BuiltNotification(n, key, null);
    }

    public BuiltNotification fromSalaryChanged(SalaryChangedEvent e, UUID recipientUserId) {
        String title = "Изменение зарплаты";
        String message = "Зарплата изменена с " + e.getPreviousSalary() + " на " + e.getNewSalary();
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.SYSTEM)
                .channel(NotificationChannel.IN_APP)
                .referenceType("EMPLOYEE")
                .referenceId(e.getEmployeeId())
                .build();
        String key = "SYSTEM:" + e.getEmployeeId() + ":" + recipientUserId;
        return new BuiltNotification(n, key, null);
    }

    // ── Attendance events ────────────────────────────────────────────────────

    public BuiltNotification fromAttendanceRecorded(AttendanceRecordedEvent e, UUID recipientUserId) {
        String title = "Опоздание";
        String message = "Опоздание зафиксировано " + e.getWorkDate();
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.ATTENDANCE_ALERT)
                .channel(NotificationChannel.IN_APP)
                .referenceType("ATTENDANCE")
                .referenceId(e.getRecordId())
                .build();
        String key = "ATTENDANCE_ALERT:" + e.getRecordId() + ":" + recipientUserId;
        return new BuiltNotification(n, key, null);
    }

    public BuiltNotification fromFraudAttemptDetected(FraudAttemptDetectedEvent e, UUID recipientUserId) {
        String title = "🚨 Подозрение на мошенничество";
        String message = "Score: " + e.getFraudScore() + ", flags: " + e.getFlags();
        String recipientEmail = recipientResolver.resolveEmail(null); // HR email from user record
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.FRAUD_ALERT)
                .channel(NotificationChannel.EMAIL)
                .referenceType("ATTENDANCE")
                .referenceId(e.getEmployeeId())
                .build();
        String key = "FRAUD_ALERT:" + e.getEmployeeId() + ":" + recipientUserId;
        EmailRequest emailReq = recipientEmail == null ? null : new EmailRequest(
                recipientEmail,
                "🚨 Подозрение на мошенничество",
                "fraud-alert",
                Map.of("fraudScore", e.getFraudScore(), "flags", e.getFlags()));
        return new BuiltNotification(n, key, emailReq);
    }

    // ── Leave events ─────────────────────────────────────────────────────────

    public BuiltNotification fromLeaveRequestCreated(LeaveRequestCreatedEvent e, UUID recipientUserId) {
        String title = "Новый запрос отпуска";
        String message = e.getDaysRequested() + " дн. с " + e.getStartDate() + " по " + e.getEndDate();
        String recipientEmail = recipientResolver.resolveEmail(e.getManagerId());
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.LEAVE_REQUEST)
                .channel(NotificationChannel.EMAIL)
                .referenceType("LEAVE_REQUEST")
                .referenceId(e.getRequestId())
                .build();
        String key = "LEAVE_REQUEST:" + e.getRequestId() + ":" + recipientUserId;
        EmailRequest emailReq = recipientEmail == null ? null : new EmailRequest(
                recipientEmail,
                "Новый запрос отпуска",
                "leave-request-created",
                Map.of("daysRequested", e.getDaysRequested(),
                       "startDate", e.getStartDate(),
                       "endDate", e.getEndDate(),
                       "leaveType", e.getLeaveType()));
        return new BuiltNotification(n, key, emailReq);
    }

    public BuiltNotification fromLeaveApproved(LeaveApprovedEvent e, UUID recipientUserId) {
        String title = "Отпуск одобрен";
        String message = "Ваш отпуск с " + e.getStartDate() + " по " + e.getEndDate() + " одобрен";
        String recipientEmail = recipientResolver.resolveEmail(e.getEmployeeId());
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.LEAVE_APPROVED)
                .channel(NotificationChannel.EMAIL)
                .referenceType("LEAVE_REQUEST")
                .referenceId(e.getRequestId())
                .build();
        String key = "LEAVE_APPROVED:" + e.getRequestId() + ":" + recipientUserId;
        EmailRequest emailReq = recipientEmail == null ? null : new EmailRequest(
                recipientEmail,
                "Отпуск одобрен",
                "leave-approved",
                Map.of("startDate", e.getStartDate(), "endDate", e.getEndDate()));
        return new BuiltNotification(n, key, emailReq);
    }

    public BuiltNotification fromLeaveRejected(LeaveRejectedEvent e, UUID recipientUserId) {
        String title = "Отпуск отклонён";
        String message = "Причина: " + e.getComment();
        String recipientEmail = recipientResolver.resolveEmail(e.getEmployeeId());
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.LEAVE_REJECTED)
                .channel(NotificationChannel.EMAIL)
                .referenceType("LEAVE_REQUEST")
                .referenceId(e.getRequestId())
                .build();
        String key = "LEAVE_REJECTED:" + e.getRequestId() + ":" + recipientUserId;
        EmailRequest emailReq = recipientEmail == null ? null : new EmailRequest(
                recipientEmail,
                "Отпуск отклонён",
                "leave-rejected",
                Map.of("comment", e.getComment() != null ? e.getComment() : ""));
        return new BuiltNotification(n, key, emailReq);
    }

    // ── Payroll events ───────────────────────────────────────────────────────

    public BuiltNotification fromPayrollJobStarted(PayrollJobStartedEvent e, UUID recipientUserId) {
        String title = "Расчёт зарплаты запущен";
        String message = "Период: " + e.getMonth() + "/" + e.getYear() + " (" + e.getEmployeeCount() + " сотр.)";
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.SYSTEM)
                .channel(NotificationChannel.IN_APP)
                .referenceType("PAYROLL_PERIOD")
                .referenceId(e.getPeriodId())
                .build();
        String key = "SYSTEM:" + e.getPeriodId() + ":started:" + recipientUserId;
        return new BuiltNotification(n, key, null);
    }

    public BuiltNotification fromPayrollJobCompleted(PayrollJobCompletedEvent e, UUID recipientUserId) {
        String title = "Расчёт зарплаты завершён";
        String message = e.getEmployeeCount() + " сотр., итого ₸" + e.getTotalNet();
        String recipientEmail = null; // HR users — email resolved externally if needed
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.PAYROLL_READY)
                .channel(NotificationChannel.EMAIL)
                .referenceType("PAYROLL_PERIOD")
                .referenceId(e.getPeriodId())
                .build();
        String key = "PAYROLL_READY:" + e.getPeriodId() + ":" + recipientUserId;
        EmailRequest emailReq = new EmailRequest(
                recipientUserId.toString(), // placeholder — listener provides real email
                "Расчёт зарплаты завершён",
                "payroll-job-completed",
                Map.of("employeeCount", e.getEmployeeCount(), "totalNet", e.getTotalNet()));
        return new BuiltNotification(n, key, emailReq);
    }

    public BuiltNotification fromPayrollAnomalyDetected(PayrollAnomalyDetectedEvent e, UUID recipientUserId) {
        String title = "⚠️ Аномалия в расчёте";
        String message = "Payslip " + e.getPayslipId() + ", score " + e.getAnomalyScore();
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.PAYROLL_ANOMALY)
                .channel(NotificationChannel.EMAIL)
                .referenceType("PAYSLIP")
                .referenceId(e.getPayslipId())
                .build();
        String key = "PAYROLL_ANOMALY:" + e.getPayslipId() + ":" + recipientUserId;
        EmailRequest emailReq = new EmailRequest(
                recipientUserId.toString(),
                "⚠️ Аномалия в расчёте зарплаты",
                "payroll-anomaly",
                Map.of("payslipId", e.getPayslipId(), "anomalyScore", e.getAnomalyScore()));
        return new BuiltNotification(n, key, emailReq);
    }

    public BuiltNotification fromPayrollPeriodApproved(PayrollPeriodApprovedEvent e, PayslipBriefDto slip, UUID recipientUserId) {
        String title = "Расчётный лист готов";
        String message = "Ваш расчётный лист за " + e.getMonth() + "/" + e.getYear() + " готов";
        String recipientEmail = recipientResolver.resolveEmail(slip.employeeId());
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.PAYSLIP_GENERATED)
                .channel(NotificationChannel.EMAIL)
                .referenceType("PAYSLIP")
                .referenceId(slip.id())
                .build();
        String key = "PAYSLIP_GENERATED:" + slip.id() + ":" + recipientUserId;
        EmailRequest emailReq = recipientEmail == null ? null : new EmailRequest(
                recipientEmail,
                "Расчётный лист за " + e.getMonth() + "/" + e.getYear(),
                "payslip-ready",
                Map.of("month", e.getMonth(), "year", e.getYear(), "netSalary", slip.netSalary()));
        return new BuiltNotification(n, key, emailReq);
    }

    // ── User events ──────────────────────────────────────────────────────────

    public BuiltNotification fromUserAccountCreated(UserAccountCreatedEvent e, UUID recipientUserId) {
        String title = "Добро пожаловать";
        String message = "Ваш аккаунт создан. Логин: " + e.getEmail();
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.SYSTEM)
                .channel(NotificationChannel.EMAIL)
                .referenceType("USER")
                .referenceId(e.getUserId())
                .build();
        String key = "SYSTEM:" + e.getUserId() + ":welcome:" + recipientUserId;
        EmailRequest emailReq = new EmailRequest(
                e.getEmail(),
                "Добро пожаловать в HRMS",
                "welcome",
                Map.of("firstName", e.getFirstName(),
                       "email", e.getEmail(),
                       "temporaryPassword", e.getTemporaryPassword(),
                       "loginUrl", baseUrl + "/login"));
        return new BuiltNotification(n, key, emailReq);
    }

    public BuiltNotification fromPasswordResetRequested(PasswordResetRequestedEvent e, UUID recipientUserId) {
        String title = "Сброс пароля";
        String message = "Ссылка для сброса отправлена на email";
        Notification n = Notification.builder()
                .userId(recipientUserId)
                .title(title)
                .message(message)
                .type(NotificationType.PASSWORD_RESET)
                .channel(NotificationChannel.EMAIL)
                .referenceType("USER")
                .referenceId(e.getUserId())
                .build();
        String key = "PASSWORD_RESET:" + e.getUserId() + ":" + recipientUserId;
        EmailRequest emailReq = new EmailRequest(
                e.getEmail(),
                "Сброс пароля — HRMS",
                "password-reset",
                Map.of("firstName", e.getFirstName(),
                       "resetToken", e.getResetToken(),
                       "resetUrl", baseUrl + "/reset-password?token=" + e.getResetToken(),
                       "ttlSeconds", e.getTtlSeconds()));
        return new BuiltNotification(n, key, emailReq);
    }
}
