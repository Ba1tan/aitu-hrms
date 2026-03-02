package kz.aitu.hrms.modules.payroll.calculator;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Immutable result of a single payroll calculation.
 * Holds every intermediate value for full transparency in payslips.
 */
@Data
@Builder
public class PayrollCalculationResult {

    // ---- Input ----
    private BigDecimal grossSalary;
    private int workedDays;
    private int totalWorkingDays;

    // ---- Adjustments ----
    private BigDecimal earnedSalary;        // Prorated if partial month
    private BigDecimal allowances;          // Bonuses, meal allowance, etc.
    private BigDecimal deductions;          // Manual deductions

    // ---- Kazakhstan Deductions (in order of calculation per Tax Code) ----
    private BigDecimal opvAmount;           // ОПВ = earnedSalary * 10%
    private BigDecimal oopvAmount;          // ООПВ = earnedSalary * 1.5% (if applicable)
    private BigDecimal taxableIncome;       // earnedSalary - OPV - MRP deduction
    private BigDecimal ipnAmount;           // ИПН = taxableIncome * 10%

    // ---- Employer Obligations (not deducted from employee) ----
    private BigDecimal soAmount;            // СО = (earnedSalary - OPV) * 3.5%
    private BigDecimal snAmount;            // СН = earnedSalary * 9.5% - SO

    // ---- Final ----
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;           // Take-home pay

    // ---- Metadata ----
    private boolean isResident;
    private boolean hasDisability;
    private int mrpUsed;                    // МРП value used in calculation
    private String calculationYear;
    private String calculationMonth;
}
