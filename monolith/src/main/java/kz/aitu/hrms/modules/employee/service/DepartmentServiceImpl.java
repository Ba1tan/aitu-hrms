package kz.aitu.hrms.modules.employee.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.modules.employee.dto.DepartmentDtos;
import kz.aitu.hrms.modules.employee.entity.Department;
import kz.aitu.hrms.modules.employee.entity.Employee;
import kz.aitu.hrms.modules.employee.repository.DepartmentRepository;
import kz.aitu.hrms.modules.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public DepartmentDtos.DepartmentResponse create(DepartmentDtos.CreateDepartmentRequest req) {
        if (departmentRepository.existsByNameAndDeletedFalse(req.getName())) {
            throw new BusinessException("Department already exists: " + req.getName());
        }
        Department dept = Department.builder()
                .name(req.getName())
                .description(req.getDescription())
                .costCenter(req.getCostCenter())
                .build();

        if (req.getParentId() != null) {
            dept.setParent(findDeptOrThrow(req.getParentId()));
        }
        if (req.getManagerId() != null) {
            dept.setManager(findEmployeeOrThrow(req.getManagerId()));
        }
        return toResponse(departmentRepository.save(dept));
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDtos.DepartmentResponse getById(UUID id) {
        return toResponse(findDeptOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDtos.DepartmentResponse> getAll() {
        return departmentRepository.findAllByDeletedFalseOrderByName()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DepartmentDtos.DepartmentResponse update(UUID id, DepartmentDtos.UpdateDepartmentRequest req) {
        Department dept = findDeptOrThrow(id);
        if (req.getName() != null) dept.setName(req.getName());
        if (req.getDescription() != null) dept.setDescription(req.getDescription());
        if (req.getCostCenter() != null) dept.setCostCenter(req.getCostCenter());
        if (req.getParentId() != null) dept.setParent(findDeptOrThrow(req.getParentId()));
        if (req.getManagerId() != null) dept.setManager(findEmployeeOrThrow(req.getManagerId()));
        return toResponse(departmentRepository.save(dept));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Department dept = findDeptOrThrow(id);
        dept.setDeleted(true);
        departmentRepository.save(dept);
    }

    private Department findDeptOrThrow(UUID id) {
        return departmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", id));
    }

    private Employee findEmployeeOrThrow(UUID id) {
        return employeeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private DepartmentDtos.DepartmentResponse toResponse(Department d) {
        DepartmentDtos.DepartmentResponse r = new DepartmentDtos.DepartmentResponse();
        r.setId(d.getId());
        r.setName(d.getName());
        r.setDescription(d.getDescription());
        r.setCostCenter(d.getCostCenter());
        if (d.getParent() != null) {
            DepartmentDtos.ParentDepartment p = new DepartmentDtos.ParentDepartment();
            p.setId(d.getParent().getId());
            p.setName(d.getParent().getName());
            r.setParent(p);
        }
        if (d.getManager() != null) {
            DepartmentDtos.ManagerInfo m = new DepartmentDtos.ManagerInfo();
            m.setId(d.getManager().getId());
            m.setFullName(d.getManager().getFullName());
            m.setEmail(d.getManager().getEmail());
            r.setManager(m);
        }
        return r;
    }
}
