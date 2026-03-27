package kz.aitu.hrms.modules.payroll.calculator;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollCalculationResult {

    // Input
    private BigDecimal grossSalary;
    private int workedDays;
    private int totalWorkingDays;

    // Adjustments
    private BigDecimal earnedSalary;        // Prorated if partial month
    private BigDecimal allowances;          // Bonuses, meal allowance, etc.
    private BigDecimal deductions;          // Manual deductions

    // Kazakhstan Deductions (in order of calculation per Tax Code)
    private BigDecimal opvAmount;           // ОПВ = earnedSalary * 10%
    private BigDecimal vosmsAmount;         // ВОСМС = earnedSalary * 2% (employee medical) — NEW 2026
    private BigDecimal oopvAmount;          // ООПВ = 0 for most employees
    private BigDecimal taxableIncome;       // earnedSalary - OPV - VOSMS - 30×MRP
    private BigDecimal ipnAmount;           // ИПН = taxableIncome * 10%

    //  Employer Obligations (not deducted from employee)
    private BigDecimal soAmount;            // СО = (earnedSalary - OPV) * 5%
    private BigDecimal snAmount;            // СН = earnedSalary * 6% (fixed, no longer -SO)
    private BigDecimal opvrAmount;          // ОПВР = earnedSalary * 3.5%

    //  Final
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;           // Final pay

    //  Metadata
    private boolean isResident;
    private boolean hasDisability;
    private int mrpUsed;                    // МРП value used in calculation
    private String calculationYear;
    private String calculationMonth;
}
