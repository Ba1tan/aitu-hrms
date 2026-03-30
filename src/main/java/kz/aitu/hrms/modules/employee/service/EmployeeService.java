package kz.aitu.hrms.modules.employee.service;

import kz.aitu.hrms.modules.employee.dto.EmployeeDtos;
import kz.aitu.hrms.modules.employee.entity.Employee;
import kz.aitu.hrms.modules.employee.entity.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {

    EmployeeDtos.EmployeeResponse create(EmployeeDtos.CreateEmployeeRequest request);

    EmployeeDtos.EmployeeResponse getById(UUID id);

    Page<EmployeeDtos.EmployeeSummary> getAll(String search, UUID departmentId,
                                               EmploymentStatus status, Pageable pageable);

    EmployeeDtos.EmployeeResponse update(UUID id, EmployeeDtos.UpdateEmployeeRequest request);

    EmployeeDtos.EmployeeResponse updateStatus(UUID id, EmployeeDtos.UpdateStatusRequest request);

    void delete(UUID id);

    List<Employee> findActiveEmployees();
}
