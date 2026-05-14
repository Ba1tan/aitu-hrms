package kz.aitu.hrms.notification.service;

import kz.aitu.hrms.common.event.*;
import kz.aitu.hrms.notification.client.dto.PayslipBriefDto;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.event.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationFactoryTest {

    @Mock
    private RecipientResolver recipientResolver;

    @InjectMocks
    private NotificationFactory factory;

    @Test
    void fromEmployeeCreated_correctTypeAndRef() {
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var event = EmployeeCreatedEvent.builder().employeeId(empId).fullName("Test User").build();
        var built = factory.fromEmployeeCreated(event, userId);
        assertThat(built.notification().getType()).isEqualTo(NotificationType.EMPLOYEE_ONBOARDED);
        assertThat(built.notification().getReferenceType()).isEqualTo("EMPLOYEE");
        assertThat(built.notification().getReferenceId()).isEqualTo(empId);
        assertThat(built.idempotencyKey()).contains("EMPLOYEE_ONBOARDED").contains(empId.toString());
        assertThat(built.emailRequest()).isNull();
    }

    @Test
    void fromEmployeeTerminated_correctType() {
        var event = EmployeeTerminatedEvent.builder().employeeId(UUID.randomUUID()).terminationDate(LocalDate.now()).build();
        var built = factory.fromEmployeeTerminated(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.EMPLOYEE_TERMINATED);
        assertThat(built.notification().getReferenceType()).isEqualTo("EMPLOYEE");
    }

    @Test
    void fromSalaryChanged_correctType() {
        var event = SalaryChangedEvent.builder().employeeId(UUID.randomUUID())
                .previousSalary(BigDecimal.valueOf(100000)).newSalary(BigDecimal.valueOf(120000)).build();
        var built = factory.fromSalaryChanged(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.SYSTEM);
    }

    @Test
    void fromAttendanceRecorded_lateStatus() {
        var event = AttendanceRecordedEvent.builder().recordId(UUID.randomUUID())
                .employeeId(UUID.randomUUID()).status("LATE").workDate(LocalDate.now()).build();
        var built = factory.fromAttendanceRecorded(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.ATTENDANCE_ALERT);
        assertThat(built.notification().getReferenceType()).isEqualTo("ATTENDANCE");
    }

    @Test
    void fromFraudAttemptDetected_correctType() {
        when(recipientResolver.resolveEmail(any())).thenReturn(null);
        var event = FraudAttemptDetectedEvent.builder().employeeId(UUID.randomUUID())
                .fraudScore(0.95).flags("SPOOFED").build();
        var built = factory.fromFraudAttemptDetected(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.FRAUD_ALERT);
        assertThat(built.notification().getReferenceType()).isEqualTo("ATTENDANCE");
    }

    @Test
    void fromLeaveRequestCreated_correctType() {
        UUID managerId = UUID.randomUUID();
        when(recipientResolver.resolveEmail(managerId)).thenReturn("mgr@test.kz");
        var event = LeaveRequestCreatedEvent.builder().requestId(UUID.randomUUID())
                .employeeId(UUID.randomUUID()).managerId(managerId).leaveType("ANNUAL")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(7)).daysRequested(7).build();
        var built = factory.fromLeaveRequestCreated(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.LEAVE_REQUEST);
        assertThat(built.notification().getReferenceType()).isEqualTo("LEAVE_REQUEST");
    }

    @Test
    void fromLeaveApproved_correctType() {
        UUID empId = UUID.randomUUID();
        when(recipientResolver.resolveEmail(empId)).thenReturn("emp@test.kz");
        var event = LeaveApprovedEvent.builder().requestId(UUID.randomUUID()).employeeId(empId)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(7)).build();
        var built = factory.fromLeaveApproved(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.LEAVE_APPROVED);
    }

    @Test
    void fromLeaveRejected_correctType() {
        UUID empId = UUID.randomUUID();
        when(recipientResolver.resolveEmail(empId)).thenReturn(null);
        var event = LeaveRejectedEvent.builder().requestId(UUID.randomUUID())
                .employeeId(empId).comment("No coverage").build();
        var built = factory.fromLeaveRejected(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.LEAVE_REJECTED);
    }

    @Test
    void fromPayrollJobStarted_correctType() {
        var event = PayrollJobStartedEvent.builder().periodId(UUID.randomUUID())
                .year(2025).month(1).employeeCount(100).build();
        var built = factory.fromPayrollJobStarted(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(built.notification().getReferenceType()).isEqualTo("PAYROLL_PERIOD");
    }

    @Test
    void fromPayrollJobCompleted_correctType() {
        var event = PayrollJobCompletedEvent.builder().periodId(UUID.randomUUID())
                .employeeCount(100).totalNet(BigDecimal.valueOf(5000000)).build();
        var built = factory.fromPayrollJobCompleted(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.PAYROLL_READY);
    }

    @Test
    void fromPayrollAnomalyDetected_correctType() {
        var event = PayrollAnomalyDetectedEvent.builder().payslipId(UUID.randomUUID())
                .employeeId(UUID.randomUUID()).anomalyScore(BigDecimal.valueOf(0.85)).build();
        var built = factory.fromPayrollAnomalyDetected(event, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.PAYROLL_ANOMALY);
        assertThat(built.notification().getReferenceType()).isEqualTo("PAYSLIP");
    }

    @Test
    void fromPayrollPeriodApproved_correctType() {
        UUID empId = UUID.randomUUID();
        UUID slipId = UUID.randomUUID();
        when(recipientResolver.resolveEmail(empId)).thenReturn("emp@test.kz");
        var event = PayrollPeriodApprovedEvent.builder().periodId(UUID.randomUUID())
                .year(2025).month(1).build();
        var slip = new PayslipBriefDto(slipId, empId, BigDecimal.valueOf(200000));
        var built = factory.fromPayrollPeriodApproved(event, slip, UUID.randomUUID());
        assertThat(built.notification().getType()).isEqualTo(NotificationType.PAYSLIP_GENERATED);
        assertThat(built.notification().getReferenceId()).isEqualTo(slipId);
    }

    @Test
    void fromUserAccountCreated_correctType() {
        UUID userId = UUID.randomUUID();
        var event = UserAccountCreatedEvent.builder().userId(userId).email("user@hrms.kz")
                .firstName("Иван").temporaryPassword("Temp123").build();
        var built = factory.fromUserAccountCreated(event, userId);
        assertThat(built.notification().getType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(built.emailRequest()).isNotNull();
        assertThat(built.emailRequest().templateName()).isEqualTo("welcome");
    }

    @Test
    void fromPasswordResetRequested_correctType() {
        UUID userId = UUID.randomUUID();
        var event = PasswordResetRequestedEvent.builder().userId(userId).email("user@hrms.kz")
                .firstName("Иван").resetToken("tok123").ttlSeconds(86400).build();
        var built = factory.fromPasswordResetRequested(event, userId);
        assertThat(built.notification().getType()).isEqualTo(NotificationType.PASSWORD_RESET);
        assertThat(built.emailRequest()).isNotNull();
        assertThat(built.emailRequest().templateName()).isEqualTo("password-reset");
    }
}
