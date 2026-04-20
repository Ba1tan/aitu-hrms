package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.employee.dto.DepartmentDtos;
import kz.aitu.hrms.employee.entity.Department;
import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.repository.DepartmentRepository;
import kz.aitu.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper mapper;

    @Override
    @Transactional
    public DepartmentDtos.DepartmentResponse create(DepartmentDtos.CreateDepartmentRequest req) {
        if (req.getCode() != null && !req.getCode().isBlank()
                && departmentRepository.existsByCodeAndDeletedFalse(req.getCode())) {
            throw new BusinessException("Department code already exists: " + req.getCode());
        }
        Department dept = Department.builder()
                .name(req.getName())
                .code(req.getCode())
                .description(req.getDescription())
                .parent(req.getParentId() != null ? requireDepartment(req.getParentId()) : null)
                .manager(req.getManagerId() != null ? requireEmployee(req.getManagerId()) : null)
                .build();
        Department saved = departmentRepository.save(dept);
        return mapper.toDepartmentResponse(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDtos.DepartmentResponse get(UUID id) {
        Department dept = requireDepartment(id);
        return mapper.toDepartmentResponse(dept, countEmployees(dept.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentResponse> list() {
        return departmentRepository.findAllByDeletedFalse().stream()
                .map(d -> mapper.toDepartmentResponse(d, countEmployees(d.getId())))
                .toList();
    }

    @Override
    @Transactional
    public DepartmentDtos.DepartmentResponse update(UUID id, DepartmentDtos.UpdateDepartmentRequest req) {
        Department dept = requireDepartment(id);
        if (req.getName() != null) dept.setName(req.getName());
        if (req.getCode() != null) {
            if (!req.getCode().equals(dept.getCode())
                    && departmentRepository.existsByCodeAndDeletedFalse(req.getCode())) {
                throw new BusinessException("Department code already exists: " + req.getCode());
            }
            dept.setCode(req.getCode());
        }
        if (req.getDescription() != null) dept.setDescription(req.getDescription());
        if (req.getParentId() != null) {
            if (req.getParentId().equals(id)) {
                throw new BusinessException("Department cannot be its own parent");
            }
            dept.setParent(requireDepartment(req.getParentId()));
        }
        if (req.getManagerId() != null) {
            dept.setManager(requireEmployee(req.getManagerId()));
        }
        return mapper.toDepartmentResponse(dept, countEmployees(dept.getId()));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Department dept = requireDepartment(id);
        long count = countEmployees(dept.getId());
        if (count > 0) {
            throw new BusinessException("Cannot delete department with assigned employees (" + count + ")");
        }
        dept.setDeleted(true);
        departmentRepository.save(dept);
    }

    private Department requireDepartment(UUID id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
    }

    private Employee requireEmployee(UUID id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private long countEmployees(UUID departmentId) {
        return employeeRepository.findAllByDeletedFalse().stream()
                .filter(e -> e.getDepartment() != null && departmentId.equals(e.getDepartment().getId()))
                .count();
    }
}