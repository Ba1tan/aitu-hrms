package kz.aitu.hrms.common.types;

/**
 * Kazakhstan disability classification used across employee/payroll services.
 *
 * Source of truth for the catalog (employee-service owns the data); payroll-service
 * uses this to compute the additional Tax Code Art. 404 deduction:
 *   - GROUP_3      → +882×МРП
 *   - GROUP_1, _2  → +5000×МРП
 *   - NONE         → no extra deduction
 *
 * Stored as the enum name in the {@code employees.disability_group} VARCHAR
 * column via {@code @Enumerated(EnumType.STRING)}; the names are part of the
 * cross-service contract — never rename without a migration.
 */
public enum DisabilityGroup {
    NONE,
    GROUP_1,
    GROUP_2,
    GROUP_3
}