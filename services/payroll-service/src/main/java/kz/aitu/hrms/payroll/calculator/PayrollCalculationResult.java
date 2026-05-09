package kz.aitu.hrms.payroll.calculator;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollCalculationResult {

    private BigDecimal grossSalary;
    private int workedDays;
    private int totalWorkingDays;

    private BigDecimal earnedSalary;        // Prorated if partial month
    private BigDecimal allowances;          // Bonuses, meal allowance, etc.
    private BigDecimal deductions;          // Manual deductions (excl. tax)

    private BigDecimal opvAmount;           // ОПВ employee pension 10%
    private BigDecimal vosmsAmount;         // ВОСМС employee medical 2%
    private BigDecimal oopvAmount;          // ООПВ — 0 for most employees
    private BigDecimal taxableIncome;       // earned − OPV − VOSMS − deduction
    private BigDecimal ipnAmount;           // ИПН income tax 10% / 20% non-resident

    private BigDecimal soAmount;            // СО 5% (employer)
    private BigDecimal snAmount;            // СН 6% (employer)
    private BigDecimal opvrAmount;          // ОПВР 3.5% (employer)

    private BigDecimal totalDeductions;
    private BigDecimal netSalary;

    private boolean resident;
    private boolean disability;
    private int mrpUsed;
    private String calculationYear;
    private String calculationMonth;
}