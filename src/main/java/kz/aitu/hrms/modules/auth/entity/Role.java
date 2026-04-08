package kz.aitu.hrms.modules.auth.entity;

public enum Role {
    SUPER_ADMIN,    // Full system access, settings, roles, users
    DIRECTOR,       // Executive dashboards, company-wide read-only analytics
    HR_MANAGER,     // HR operations, payroll processing, leave final approval, all reports
    HR_SPECIALIST,  // Employee management, attendance, leave processing (no payroll)
    ACCOUNTANT,     // View/verify payroll, mark paid, Form 200.00, financial reports
    MANAGER,        // Approve team leave, view team attendance/reports
    TEAM_LEAD,      // View team attendance, approve team leave (narrower than MANAGER)
    EMPLOYEE        // Self-service: payslips, leave requests, attendance, profile
}
