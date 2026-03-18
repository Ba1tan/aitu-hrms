package kz.aitu.hrms.modules.employee.service;

import kz.aitu.hrms.modules.employee.entity.Employee;

import java.util.List;

/**
 * Minimal interface — only what payroll module needs right now.
 * Full CRUD methods will be added when employee module is implemented
 */
public interface EmployeeService {

    /** Returns all ACTIVE non-deleted employees. Used by payroll to generate payslips. */
    List<Employee> findActiveEmployees();
}
