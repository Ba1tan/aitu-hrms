package kz.aitu.hrms.modules.auth.entity;

public enum Role {
    SUPER_ADMIN,    // Full system access
    HR_MANAGER,     // HR operations, payroll processing
    ACCOUNTANT,     // View payroll, generate reports
    EMPLOYEE        // Self-service only
}
