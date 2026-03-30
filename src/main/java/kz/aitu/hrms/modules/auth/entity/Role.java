package kz.aitu.hrms.modules.auth.entity;

public enum Role {
    SUPER_ADMIN,    // Full system access
    HR_MANAGER,     // HR operations, payroll processing, leave final approval
    ACCOUNTANT,     // View payroll, generate reports, mark periods as paid
    MANAGER,        // Approve/reject leave for direct reports, view team data
    EMPLOYEE        // Self-service: payslips, leave requests, attendance
}
