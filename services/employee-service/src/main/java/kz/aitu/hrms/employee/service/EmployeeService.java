package kz.aitu.hrms.employee.service;

import kz.aitu.hrms.employee.dto.EmployeeDtos;
import kz.aitu.hrms.employee.entity.EmploymentStatus;
import kz.aitu.hrms.employee.entity.EmploymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EmployeeService {

    EmployeeDtos.EmployeeResponse create(EmployeeDtos.CreateEmployeeRequest req);

    EmployeeDtos.EmployeeResponse get(UUID id);

    Page<EmployeeDtos.EmployeeSummary> list(String search, UUID departmentId,
                                            EmploymentStatus status, EmploymentType type,
                                            Pageable pageable);

    EmployeeDtos.EmployeeResponse update(UUID id, EmployeeDtos.UpdateEmployeeRequest req);

    EmployeeDtos.EmployeeResponse updateStatus(UUID id, EmployeeDtos.UpdateStatusRequest req);

    void delete(UUID id);

    EmployeeDtos.EmployeeResponse terminate(UUID id, EmployeeDtos.TerminateRequest req);

    EmployeeDtos.EmployeeResponse createAccount(UUID id);
}