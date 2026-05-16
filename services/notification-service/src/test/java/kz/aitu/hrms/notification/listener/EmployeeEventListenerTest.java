package kz.aitu.hrms.notification.listener;

import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.common.event.EmployeeTerminatedEvent;
import kz.aitu.hrms.common.event.SalaryChangedEvent;
import kz.aitu.hrms.notification.domain.Notification;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.service.NotificationFactory;
import kz.aitu.hrms.notification.service.NotificationService;
import kz.aitu.hrms.notification.service.RecipientResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeEventListenerTest {

    @Mock private NotificationFactory factory;
    @Mock private NotificationService service;
    @Mock private RecipientResolver recipients;

    @InjectMocks private EmployeeEventListener listener;

    private NotificationFactory.BuiltNotification built(UUID userId) {
        return new NotificationFactory.BuiltNotification(
                Notification.builder().userId(userId).title("T").message("M")
                        .type(NotificationType.EMPLOYEE_ONBOARDED).build(),
                "key", null);
    }

    @Test
    void onEmployeeCreated_happyPath_createsNotifications() {
        UUID userId = UUID.randomUUID();
        EmployeeCreatedEvent event = EmployeeCreatedEvent.builder()
                .employeeId(UUID.randomUUID()).fullName("Ivan Ivanov")
                .email("ivan@test.kz").salary(BigDecimal.valueOf(100000)).build();
        when(recipients.resolveUserIdsByPermission("EMPLOYEE_CREATE")).thenReturn(List.of(userId));
        when(factory.fromEmployeeCreated(event, userId)).thenReturn(built(userId));

        listener.onEmployeeCreated(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onEmployeeCreated_recipientsThrows_doesNotPropagate() {
        EmployeeCreatedEvent event = EmployeeCreatedEvent.builder()
                .employeeId(UUID.randomUUID()).build();
        when(recipients.resolveUserIdsByPermission(any())).thenThrow(new RuntimeException("down"));

        listener.onEmployeeCreated(event);

        verify(service, never()).create(any(), any(), any());
    }

    @Test
    void onEmployeeTerminated_happyPath_createsNotifications() {
        UUID userId = UUID.randomUUID();
        EmployeeTerminatedEvent event = EmployeeTerminatedEvent.builder()
                .employeeId(UUID.randomUUID()).terminationDate(LocalDate.now()).reason("Resigned").build();
        var builtTerminated = new NotificationFactory.BuiltNotification(
                Notification.builder().userId(userId).title("T").message("M")
                        .type(NotificationType.EMPLOYEE_TERMINATED).build(),
                "key2", null);
        when(recipients.resolveUserIdsByPermission("EMPLOYEE_CREATE")).thenReturn(List.of(userId));
        when(factory.fromEmployeeTerminated(event, userId)).thenReturn(builtTerminated);

        listener.onEmployeeTerminated(event);

        verify(service).create(any(), any(), any());
    }

    @Test
    void onSalaryChanged_happyPath_createsNotifications() {
        UUID empId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SalaryChangedEvent event = SalaryChangedEvent.builder()
                .employeeId(empId).previousSalary(BigDecimal.valueOf(100000))
                .newSalary(BigDecimal.valueOf(120000)).build();
        var builtSalary = new NotificationFactory.BuiltNotification(
                Notification.builder().userId(userId).title("T").message("M")
                        .type(NotificationType.SYSTEM).build(),
                "key3", null);
        when(recipients.resolveUserIds(empId)).thenReturn(List.of(userId));
        when(factory.fromSalaryChanged(event, userId)).thenReturn(builtSalary);

        listener.onSalaryChanged(event);

        verify(service).create(any(), any(), any());
    }
}
