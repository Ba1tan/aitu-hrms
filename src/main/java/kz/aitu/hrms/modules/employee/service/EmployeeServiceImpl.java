package kz.aitu.hrms.modules.employee.service;

import kz.aitu.hrms.modules.employee.entity.Employee;
import kz.aitu.hrms.modules.employee.entity.EmploymentStatus;
import kz.aitu.hrms.modules.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Stub implementation — only findActiveEmployees() is wired.
 * Full implementation comes with employee module
 */
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findActiveEmployees() {
        return employeeRepository.findByStatusAndDeletedFalse(EmploymentStatus.ACTIVE);
    }
}
