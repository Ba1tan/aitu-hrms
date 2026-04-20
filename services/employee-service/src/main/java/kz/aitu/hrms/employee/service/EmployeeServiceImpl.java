package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.event.EmployeeCreatedEvent;
import kz.aitu.hrms.common.event.EmployeeTerminatedEvent;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.dto.EmployeeDtos;
import kz.aitu.hrms.employee.entity.Department;
import kz.aitu.hrms.employee.entity.DisabilityGroup;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.entity.EmploymentStatus;
import kz.aitu.hrms.employee.entity.EmploymentType;
import kz.aitu.hrms.employee.entity.Position;
import kz.aitu.hrms.employee.repository.DepartmentRepository;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import kz.aitu.hrms.employee.repository.PositionRepository;
import kz.aitu.hrms.employee.security.EmployeeAccessControl;
import kz.aitu.hrms.employee.security.EmployeeScope;
import kz.aitu.hrms.employee.util.IinValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private static final DateTimeFormatter YM = DateTimeFormatter.ofPattern("yyyyMM");

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EventPublisher eventPublisher;
    private final EmployeeAccessControl accessControl;
    private final EmployeeMapper mapper;

    @Override
    @Transactional
    public EmployeeDtos.EmployeeResponse create(EmployeeDtos.CreateEmployeeRequest req) {
        if (req.getIin() != null && !IinValidator.isValid(req.getIin())) {
            throw new BusinessException("Invalid IIN check digit");
        }
        if (req.getEmail() != null && employeeRepository.existsByEmailAndDeletedFalse(req.getEmail())) {
            throw new BusinessException("Employee with email already exists: " + req.getEmail());
        }
        if (req.getIin() != null && employeeRepository.existsByIinAndDeletedFalse(req.getIin())) {
            throw new BusinessException("Employee with IIN already exists");
        }

        Employee emp = Employee.builder()
                .employeeNumber(generateEmployeeNumber())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .middleName(req.getMiddleName())
                .email(req.getEmail())
                .iin(req.getIin())
                .phone(req.getPhone())
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender())
                .hireDate(req.getHireDate())
                .status(EmploymentStatus.ACTIVE)
                .employmentType(req.getEmploymentType())
                .baseSalary(req.getBaseSalary())
                .disabilityGroup(req.getDisabilityGroup() != null ? req.getDisabilityGroup() : DisabilityGroup.NONE)
                .resident(req.getIsResident() == null || req.getIsResident())
                .pensioner(Boolean.TRUE.equals(req.getIsPensioner()))
                .address(req.getAddress())
                .build();

        if (req.getDepartmentId() != null) emp.setDepartment(requireDepartment(req.getDepartmentId()));
        if (req.getPositionId() != null) emp.setPosition(requirePosition(req.getPositionId()));
        if (req.getManagerId() != null) emp.setManager(requireEmployee(req.getManagerId()));

        Employee saved = employeeRepository.save(emp);
        log.info("Employee created: {} ({})", saved.getFullName(), saved.getEmployeeNumber());

        if (Boolean.TRUE.equals(req.getCreateAccount()) && saved.getEmail() != null) {
            publishCreated(saved);
        }
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDtos.EmployeeResponse get(UUID id) {
        Employee emp = requireEmployee(id);
        accessControl.assertCanView(emp);
        return mapper.toResponse(emp);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDtos.EmployeeSummary> list(String search, UUID departmentId,
                                                   EmploymentStatus status, EmploymentType type,
                                                   Pageable pageable) {
        EmployeeScope scope = accessControl.resolveScope();
        UUID scopeManagerId = scope.getKind() == EmployeeScope.Kind.TEAM ? scope.getCurrentEmployeeId() : null;
        UUID scopeSelfId = scope.getKind() == EmployeeScope.Kind.SELF ? scope.getCurrentEmployeeId() : null;
        if (scope.getKind() != EmployeeScope.Kind.ALL && scope.getCurrentEmployeeId() == null) {
            return Page.empty(pageable);
        }
        return employeeRepository.search(search, departmentId, status, type,
                        scopeManagerId, scopeSelfId, pageable)
                .map(mapper::toSummary);
    }

    @Override
    @Transactional
    public EmployeeDtos.EmployeeResponse update(UUID id, EmployeeDtos.UpdateEmployeeRequest req) {
        Employee emp = requireEmployee(id);

        if (req.getFirstName() != null) emp.setFirstName(req.getFirstName());
        if (req.getLastName() != null) emp.setLastName(req.getLastName());
        if (req.getMiddleName() != null) emp.setMiddleName(req.getMiddleName());
        if (req.getPhone() != null) emp.setPhone(req.getPhone());
        if (req.getDateOfBirth() != null) emp.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender() != null) emp.setGender(req.getGender());
        if (req.getEmploymentType() != null) emp.setEmploymentType(req.getEmploymentType());
        if (req.getBaseSalary() != null) emp.setBaseSalary(req.getBaseSalary());
        if (req.getDisabilityGroup() != null) emp.setDisabilityGroup(req.getDisabilityGroup());
        if (req.getIsResident() != null) emp.setResident(req.getIsResident());
        if (req.getIsPensioner() != null) emp.setPensioner(req.getIsPensioner());
        if (req.getAddress() != null) emp.setAddress(req.getAddress());

        if (req.getEmail() != null && !req.getEmail().equals(emp.getEmail())) {
            if (employeeRepository.existsByEmailAndDeletedFalse(req.getEmail())) {
                throw new BusinessException("Email already in use: " + req.getEmail());
            }
            emp.setEmail(req.getEmail());
        }
        if (req.getIin() != null && !req.getIin().equals(emp.getIin())) {
            if (!IinValidator.isValid(req.getIin())) {
                throw new BusinessException("Invalid IIN check digit");
            }
            if (employeeRepository.existsByIinAndDeletedFalse(req.getIin())) {
                throw new BusinessException("IIN already in use");
            }
            emp.setIin(req.getIin());
        }
        if (req.getDepartmentId() != null) emp.setDepartment(requireDepartment(req.getDepartmentId()));
        if (req.getPositionId() != null) emp.setPosition(requirePosition(req.getPositionId()));
        if (req.getManagerId() != null) {
            if (req.getManagerId().equals(id)) {
                throw new BusinessException("Employee cannot be their own manager");
            }
            emp.setManager(requireEmployee(req.getManagerId()));
        }
        return mapper.toResponse(emp);
    }

    @Override
    @Transactional
    public EmployeeDtos.EmployeeResponse updateStatus(UUID id, EmployeeDtos.UpdateStatusRequest req) {
        Employee emp = requireEmployee(id);
        if (req.getStatus() == EmploymentStatus.TERMINATED) {
            throw new BusinessException("Use POST /terminate to end employment");
        }
        emp.setStatus(req.getStatus());
        log.info("Employee {} status → {}", emp.getEmployeeNumber(), req.getStatus());
        return mapper.toResponse(emp);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Employee emp = requireEmployee(id);
        emp.setDeleted(true);
        log.info("Employee soft-deleted: {}", emp.getEmployeeNumber());
    }

    @Override
    @Transactional
    public EmployeeDtos.EmployeeResponse terminate(UUID id, EmployeeDtos.TerminateRequest req) {
        Employee emp = requireEmployee(id);
        if (emp.getStatus() == EmploymentStatus.TERMINATED) {
            throw new BusinessException("Employee is already terminated");
        }
        emp.setStatus(EmploymentStatus.TERMINATED);
        emp.setTerminationDate(req.getTerminationDate());
        emp.setTerminationReason(req.getReason());
        log.info("Employee terminated: {} on {}", emp.getEmployeeNumber(), req.getTerminationDate());

        eventPublisher.publish(EmployeeTerminatedEvent.builder()
                .employeeId(emp.getId())
                .terminationDate(req.getTerminationDate())
                .reason(req.getReason())
                .build());
        return mapper.toResponse(emp);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDtos.EmployeeResponse createAccount(UUID id) {
        Employee emp = requireEmployee(id);
        if (emp.getEmail() == null || emp.getEmail().isBlank()) {
            throw new BusinessException("Employee has no email; cannot create account");
        }
        publishCreated(emp);
        return mapper.toResponse(emp);
    }

    private void publishCreated(Employee saved) {
        eventPublisher.publish(EmployeeCreatedEvent.builder()
                .employeeId(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .salary(saved.getBaseSalary())
                .departmentId(saved.getDepartment() != null ? saved.getDepartment().getId() : null)
                .build());
    }

    private String generateEmployeeNumber() {
        long n = employeeRepository.countByDeletedFalse() + 1;
        return String.format("EMP-%s-%03d", YearMonth.now().format(YM), n);
    }

    private Employee requireEmployee(UUID id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private Department requireDepartment(UUID id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private Position requirePosition(UUID id) {
        return positionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found: " + id));
    }
}