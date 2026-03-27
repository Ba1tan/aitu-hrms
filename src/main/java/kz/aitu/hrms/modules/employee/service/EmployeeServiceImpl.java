package kz.aitu.hrms.modules.employee.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.modules.employee.dto.EmployeeDtos;
import kz.aitu.hrms.modules.employee.entity.Department;
import kz.aitu.hrms.modules.employee.entity.Employee;
import kz.aitu.hrms.modules.employee.entity.EmploymentStatus;
import kz.aitu.hrms.modules.employee.entity.Position;
import kz.aitu.hrms.modules.employee.repository.DepartmentRepository;
import kz.aitu.hrms.modules.employee.repository.EmployeeRepository;
import kz.aitu.hrms.modules.employee.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;

    @Override
    @Transactional
    public EmployeeDtos.EmployeeResponse create(EmployeeDtos.CreateEmployeeRequest req) {
        if (employeeRepository.existsByEmailAndDeletedFalse(req.getEmail())) {
            throw new BusinessException("Employee with email already exists: " + req.getEmail());
        }
        if (req.getIin() != null && employeeRepository.existsByIinAndDeletedFalse(req.getIin())) {
            throw new BusinessException("Employee with IIN already exists: " + req.getIin());
        }

        Employee emp = Employee.builder()
                .employeeNumber(generateEmployeeNumber())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .middleName(req.getMiddleName())
                .email(req.getEmail())
                .iin(req.getIin())
                .phone(req.getPhone())
                .hireDate(req.getHireDate())
                .dateOfBirth(req.getDateOfBirth())
                .employmentType(req.getEmploymentType())
                .baseSalary(req.getBaseSalary())
                .status(EmploymentStatus.ACTIVE)
                .bankAccount(req.getBankAccount())
                .bankName(req.getBankName())
                .resident(req.isResident())
                .hasDisability(req.isHasDisability())
                .pensioner(req.isPensioner())
                .build();

        if (req.getDepartmentId() != null) {
            emp.setDepartment(findDepartmentOrThrow(req.getDepartmentId()));
        }
        if (req.getPositionId() != null) {
            emp.setPosition(findPositionOrThrow(req.getPositionId()));
        }
        if (req.getManagerId() != null) {
            emp.setManager(findEmployeeOrThrow(req.getManagerId()));
        }

        Employee saved = employeeRepository.save(emp);
        log.info("Employee created: {} ({})", saved.getFullName(), saved.getEmployeeNumber());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDtos.EmployeeResponse getById(UUID id) {
        return toResponse(findEmployeeOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDtos.EmployeeSummary> getAll(String search, UUID departmentId,
                                                      EmploymentStatus status, Pageable pageable) {
        return employeeRepository.searchEmployees(search, departmentId, status, pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional
    public EmployeeDtos.EmployeeResponse update(UUID id, EmployeeDtos.UpdateEmployeeRequest req) {
        Employee emp = findEmployeeOrThrow(id);

        if (req.getFirstName() != null) emp.setFirstName(req.getFirstName());
        if (req.getLastName() != null) emp.setLastName(req.getLastName());
        if (req.getMiddleName() != null) emp.setMiddleName(req.getMiddleName());
        if (req.getPhone() != null) emp.setPhone(req.getPhone());
        if (req.getEmploymentType() != null) emp.setEmploymentType(req.getEmploymentType());
        if (req.getBaseSalary() != null) emp.setBaseSalary(req.getBaseSalary());
        if (req.getBankAccount() != null) emp.setBankAccount(req.getBankAccount());
        if (req.getBankName() != null) emp.setBankName(req.getBankName());
        if (req.getResident() != null) emp.setResident(req.getResident());
        if (req.getHasDisability() != null) emp.setHasDisability(req.getHasDisability());
        if (req.getPensioner() != null) emp.setPensioner(req.getPensioner());

        if (req.getEmail() != null && !req.getEmail().equals(emp.getEmail())) {
            if (employeeRepository.existsByEmailAndDeletedFalse(req.getEmail())) {
                throw new BusinessException("Email already in use: " + req.getEmail());
            }
            emp.setEmail(req.getEmail());
        }
        if (req.getDepartmentId() != null) {
            emp.setDepartment(findDepartmentOrThrow(req.getDepartmentId()));
        }
        if (req.getPositionId() != null) {
            emp.setPosition(findPositionOrThrow(req.getPositionId()));
        }
        if (req.getManagerId() != null) {
            if (req.getManagerId().equals(id)) {
                throw new BusinessException("Employee cannot be their own manager");
            }
            emp.setManager(findEmployeeOrThrow(req.getManagerId()));
        }

        return toResponse(employeeRepository.save(emp));
    }

    @Override
    @Transactional
    public EmployeeDtos.EmployeeResponse updateStatus(UUID id, EmployeeDtos.UpdateStatusRequest req) {
        Employee emp = findEmployeeOrThrow(id);
        if (req.getStatus() == EmploymentStatus.TERMINATED && req.getTerminationDate() == null) {
            throw new BusinessException("Termination date is required when terminating an employee");
        }
        emp.setStatus(req.getStatus());
        if (req.getTerminationDate() != null) {
            emp.setTerminationDate(req.getTerminationDate());
        }
        log.info("Employee {} status changed to {}", emp.getEmployeeNumber(), req.getStatus());
        return toResponse(employeeRepository.save(emp));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Employee emp = findEmployeeOrThrow(id);
        emp.setDeleted(true);
        employeeRepository.save(emp);
        log.info("Employee soft-deleted: {}", emp.getEmployeeNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findActiveEmployees() {
        return employeeRepository.findByStatusAndDeletedFalse(EmploymentStatus.ACTIVE);
    }

    // helpers

    private Employee findEmployeeOrThrow(UUID id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private Department findDepartmentOrThrow(UUID id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private Position findPositionOrThrow(UUID id) {
        return positionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position", id));
    }

    private String generateEmployeeNumber() {
        return employeeRepository.findMaxEmployeeNumber()
                .map(max -> {
                    // max is like "EMP-0042", extract number and increment
                    try {
                        int n = Integer.parseInt(max.replace("EMP-", ""));
                        return String.format("EMP-%04d", n + 1);
                    } catch (NumberFormatException e) {
                        return String.format("EMP-%04d", employeeRepository.countActive() + 1);
                    }
                })
                .orElse("EMP-0001");
    }

    private EmployeeDtos.DepartmentSummary toDepartmentSummary(Department dept) {
        if (dept == null) {
            return null;
        }
        EmployeeDtos.DepartmentSummary deptSum = new EmployeeDtos.DepartmentSummary();
        deptSum.setId(dept.getId());
        deptSum.setName(dept.getName());
        return deptSum;
    }

    private EmployeeDtos.PositionSummary toPositionSummary(Position pos) {
        if (pos == null) {
            return null;
        }
        EmployeeDtos.PositionSummary posSum = new EmployeeDtos.PositionSummary();
        posSum.setId(pos.getId());
        posSum.setTitle(pos.getTitle());
        return posSum;
    }

    private EmployeeDtos.ManagerSummary toManagerSummary(Employee mgr) {
        if (mgr == null) {
            return null;
        }
        EmployeeDtos.ManagerSummary mgrSum = new EmployeeDtos.ManagerSummary();
        mgrSum.setId(mgr.getId());
        mgrSum.setFullName( mgr.getFullName());
        mgrSum.setEmail(mgr.getEmail());
        return mgrSum;
    }

    private EmployeeDtos.EmployeeResponse toResponse(Employee e) {
        EmployeeDtos.EmployeeResponse r = new EmployeeDtos.EmployeeResponse();
        r.setId(e.getId());
        r.setEmployeeNumber(e.getEmployeeNumber());
        r.setFirstName(e.getFirstName());
        r.setLastName(e.getLastName());
        r.setMiddleName(e.getMiddleName());
        r.setFullName(e.getFullName());
        r.setEmail(e.getEmail());
        r.setIin(e.getIin());
        r.setPhone(e.getPhone());
        r.setHireDate(e.getHireDate());
        r.setTerminationDate(e.getTerminationDate());
        r.setDateOfBirth(e.getDateOfBirth());
        r.setStatus(e.getStatus());
        r.setEmploymentType(e.getEmploymentType());
        r.setBaseSalary(e.getBaseSalary());
        r.setBankAccount(e.getBankAccount());
        r.setBankName(e.getBankName());
        r.setResident(e.isResident());
        r.setHasDisability(e.isHasDisability());
        r.setPensioner(e.isPensioner());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        r.setDepartment(toDepartmentSummary(e.getDepartment()));
        r.setPosition(toPositionSummary(e.getPosition()));
        r.setManager(toManagerSummary(e.getManager()));
        return r;
    }

    private EmployeeDtos.EmployeeSummary toSummary(Employee e) {
        EmployeeDtos.EmployeeSummary s = new EmployeeDtos.EmployeeSummary();
        s.setId(e.getId());
        s.setEmployeeNumber(e.getEmployeeNumber());
        s.setFirstName(e.getFirstName());
        s.setLastName(e.getLastName());
        s.setFullName(e.getFullName());
        s.setEmail(e.getEmail());
        s.setStatus(e.getStatus());
        s.setDepartment(toDepartmentSummary(e.getDepartment()));
        s.setPosition(toPositionSummary(e.getPosition()));
        return s;
    }
}
