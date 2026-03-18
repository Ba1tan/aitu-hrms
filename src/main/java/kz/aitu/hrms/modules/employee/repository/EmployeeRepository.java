package kz.aitu.hrms.modules.employee.repository;

import kz.aitu.hrms.modules.employee.entity.Employee;
import kz.aitu.hrms.modules.employee.entity.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Minimal repository — only what the payroll stub needs.
 * Full methods will be added with the employee module
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    // Used by EmployeeServiceImpl → PayrollService to get employees for payslip generation
    List<Employee> findByStatusAndDeletedFalse(EmploymentStatus status);
}
