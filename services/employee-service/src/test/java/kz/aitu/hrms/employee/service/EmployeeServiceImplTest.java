package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.common.event.EmployeeTerminatedEvent;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.dto.EmployeeDtos;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.entity.EmploymentStatus;
import kz.aitu.hrms.employee.repository.DepartmentRepository;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import kz.aitu.hrms.employee.repository.PositionRepository;
import kz.aitu.hrms.employee.security.EmployeeAccessControl;
import kz.aitu.hrms.employee.security.EmployeeScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    private static final String VALID_IIN = "910915300123";

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private EmployeeAccessControl accessControl;

    private EmployeeMapper mapper;
    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = new EmployeeMapper();
        service = new EmployeeServiceImpl(
                employeeRepository, departmentRepository, positionRepository,
                eventPublisher, accessControl, mapper);
    }

    @Test
    void createRejectsInvalidIin() {
        EmployeeDtos.CreateEmployeeRequest req = baseCreateRequest();
        req.setIin("123456789012"); // malformed check digit

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid IIN");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEmail() {
        EmployeeDtos.CreateEmployeeRequest req = baseCreateRequest();
        when(employeeRepository.existsByEmailAndDeletedFalse(req.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email already exists");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createSavesAndGeneratesEmployeeNumberWithoutPublishingWhenCreateAccountFalse() {
        EmployeeDtos.CreateEmployeeRequest req = baseCreateRequest();
        when(employeeRepository.existsByEmailAndDeletedFalse(req.getEmail())).thenReturn(false);
        when(employeeRepository.existsByIinAndDeletedFalse(req.getIin())).thenReturn(false);
        when(employeeRepository.countByDeletedFalse()).thenReturn(4L);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        EmployeeDtos.EmployeeResponse resp = service.create(req);

        assertThat(resp.getEmployeeNumber()).matches("EMP-\\d{6}-005");
        assertThat(resp.getStatus()).isEqualTo(EmploymentStatus.ACTIVE);
        verify(eventPublisher, never()).publish(any(EmployeeCreatedEvent.class));
    }

    @Test
    void createPublishesEventWhenCreateAccountTrue() {
        EmployeeDtos.CreateEmployeeRequest req = baseCreateRequest();
        req.setCreateAccount(true);
        when(employeeRepository.existsByEmailAndDeletedFalse(req.getEmail())).thenReturn(false);
        when(employeeRepository.existsByIinAndDeletedFalse(req.getIin())).thenReturn(false);
        when(employeeRepository.countByDeletedFalse()).thenReturn(0L);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        service.create(req);

        ArgumentCaptor<EmployeeCreatedEvent> captor = ArgumentCaptor.forClass(EmployeeCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo(req.getEmail());
    }

    @Test
    void updateStatusRejectsTerminatedStatus() {
        Employee emp = existingEmployee();
        when(employeeRepository.findByIdAndDeletedFalse(emp.getId())).thenReturn(Optional.of(emp));

        EmployeeDtos.UpdateStatusRequest req = new EmployeeDtos.UpdateStatusRequest();
        req.setStatus(EmploymentStatus.TERMINATED);

        assertThatThrownBy(() -> service.updateStatus(emp.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("/terminate");
    }

    @Test
    void terminateUpdatesEmployeeAndPublishesEvent() {
        Employee emp = existingEmployee();
        when(employeeRepository.findByIdAndDeletedFalse(emp.getId())).thenReturn(Optional.of(emp));

        EmployeeDtos.TerminateRequest req = new EmployeeDtos.TerminateRequest();
        req.setTerminationDate(LocalDate.now());
        req.setReason("Contract end");

        service.terminate(emp.getId(), req);

        assertThat(emp.getStatus()).isEqualTo(EmploymentStatus.TERMINATED);
        assertThat(emp.getTerminationReason()).isEqualTo("Contract end");

        ArgumentCaptor<EmployeeTerminatedEvent> captor = ArgumentCaptor.forClass(EmployeeTerminatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEmployeeId()).isEqualTo(emp.getId());
    }

    @Test
    void getThrowsWhenEmployeeMissing() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listUsesManagerIdWhenScopeIsTeam() {
        UUID managerId = UUID.randomUUID();
        when(accessControl.resolveScope())
                .thenReturn(new EmployeeScope(EmployeeScope.Kind.TEAM, managerId));
        when(employeeRepository.search(isNull(), isNull(), isNull(), isNull(),
                eq(managerId), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(existingEmployee())));

        Page<EmployeeDtos.EmployeeSummary> page =
                service.list(null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        verify(employeeRepository).search(isNull(), isNull(), isNull(), isNull(),
                eq(managerId), isNull(), any());
    }

    @Test
    void listUsesSelfIdWhenScopeIsSelf() {
        UUID selfId = UUID.randomUUID();
        when(accessControl.resolveScope())
                .thenReturn(new EmployeeScope(EmployeeScope.Kind.SELF, selfId));
        when(employeeRepository.search(isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(selfId), any()))
                .thenReturn(Page.empty());

        service.list(null, null, null, null, PageRequest.of(0, 20));

        verify(employeeRepository).search(isNull(), isNull(), isNull(), isNull(),
                isNull(), eq(selfId), any());
    }

    @Test
    void listReturnsEmptyWhenScopedUserHasNoResolvedId() {
        when(accessControl.resolveScope())
                .thenReturn(new EmployeeScope(EmployeeScope.Kind.SELF, null));

        Page<EmployeeDtos.EmployeeSummary> page =
                service.list(null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        verify(employeeRepository, never()).search(any(), any(), any(), any(), any(), any(), any());
    }

    private EmployeeDtos.CreateEmployeeRequest baseCreateRequest() {
        EmployeeDtos.CreateEmployeeRequest req = new EmployeeDtos.CreateEmployeeRequest();
        req.setFirstName("Aslan");
        req.setLastName("Nursultan");
        req.setEmail("aslan@example.com");
        req.setIin(VALID_IIN);
        req.setHireDate(LocalDate.now());
        req.setBaseSalary(new BigDecimal("500000.00"));
        return req;
    }

    private Employee existingEmployee() {
        Employee e = Employee.builder()
                .employeeNumber("EMP-202601-001")
                .firstName("Aslan")
                .lastName("Nursultan")
                .email("aslan@example.com")
                .iin(VALID_IIN)
                .hireDate(LocalDate.now().minusYears(1))
                .status(EmploymentStatus.ACTIVE)
                .baseSalary(new BigDecimal("500000.00"))
                .build();
        e.setId(UUID.randomUUID());
        return e;
    }
}