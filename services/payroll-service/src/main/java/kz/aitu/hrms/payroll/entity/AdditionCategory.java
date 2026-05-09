package kz.aitu.hrms.payroll.entity;

/**
 * Categories of additions. Must stay aligned with the CHECK constraint in
 * V1__init_payroll_schema.sql.
 */
public enum AdditionCategory {
    MEAL_ALLOWANCE,
    TRANSPORT,
    OVERTIME,
    BONUS_PERFORMANCE,
    BONUS_HOLIDAY,
    FINE,
    ADVANCE_REPAYMENT,
    TAX_ADJUSTMENT,
    INSURANCE,
    OTHER
}