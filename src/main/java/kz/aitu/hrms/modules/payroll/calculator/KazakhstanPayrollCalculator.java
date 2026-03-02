package kz.aitu.hrms.modules.payroll.calculator;

import kz.aitu.hrms.modules.employee.entity.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kazakhstan payroll calculator.
 *
 * Calculation order per Kazakhstan Tax Code (Налоговый Кодекс РК):
 *
 * Step 1: Earned salary (prorate if partial month)
 * Step 2: OPV = earnedSalary × 10%  (ОПВ, capped at 50 MRP)
 * Step 3: OOPV = earnedSalary × 1.5% (ООПВ, if applicable)
 * Step 4: Taxable income = earnedSalary - OPV - (1 MRP standard deduction for residents)
 * Step 5: IPN = taxableIncome × 10%  (ИПН, Income Tax)
 * Step 6: Net = earnedSalary - OPV - OOPV - IPN + allowances - otherDeductions
 *
 * Employer-side (not deducted from employee):
 * Step 7: SO = (earnedSalary - OPV) × 3.5%  (СО, Social Contribution)
 * Step 8: SN = earnedSalary × 9.5% - SO      (СН, Social Tax)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KazakhstanPayrollCalculator {

    @Value("${app.payroll.ipn-rate}")
    private BigDecimal ipnRate;

    @Value("${app.payroll.opv-rate}")
    private BigDecimal opvRate;

    @Value("${app.payroll.so-rate}")
    private BigDecimal soRate;

    @Value("${app.payroll.sn-rate}")
    private BigDecimal snRate;

    @Value("${app.payroll.oopv-rate}")
    private BigDecimal oopvRate;

    @Value("${app.payroll.mrp}")
    private int mrp;

    @Value("${app.payroll.min-wage}")
    private int minWage;

    private static final int OPV_CAP_MRP = 50;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Main calculation entry point.
     *
     * @param employee      the employee being paid
     * @param workedDays    actual days worked this month
     * @param totalDays     total working days in the month
     * @param allowances    additional allowances/bonuses (can be ZERO)
     * @param otherDeductions  additional deductions (can be ZERO)
     * @param year          calculation year (for records)
     * @param month         calculation month name (for records)
     */
    public PayrollCalculationResult calculate(
            Employee employee,
            int workedDays,
            int totalDays,
            BigDecimal allowances,
            BigDecimal otherDeductions,
            String year,
            String month) {

        BigDecimal grossSalary = employee.getBaseSalary();
        boolean isResident = employee.isResident();
        boolean hasDisability = employee.isHasDisability();
        boolean isPensioner = employee.isPensioner();

        // Step 1: Prorate salary
        BigDecimal earnedSalary = calculateEarnedSalary(grossSalary, workedDays, totalDays);

        // Step 2: OPV (Пенсионные взносы)
        BigDecimal opvAmount = BigDecimal.ZERO;
        if (!isPensioner) {
            opvAmount = calculateOpv(earnedSalary);
        }

        // Step 3: OOPV (Обязательные профессиональные пенсионные взносы)
        BigDecimal oopvAmount = BigDecimal.ZERO; // enabled per position if needed

        // Step 4: Taxable income
        BigDecimal mrpDeduction = isResident ? BigDecimal.valueOf(mrp) : BigDecimal.ZERO;
        if (hasDisability) {
            mrpDeduction = mrpDeduction.add(BigDecimal.valueOf((long) mrp * 882)); // 882 MRP for disability
        }
        BigDecimal taxableIncome = earnedSalary.subtract(opvAmount).subtract(mrpDeduction);
        if (taxableIncome.compareTo(BigDecimal.ZERO) < 0) {
            taxableIncome = BigDecimal.ZERO;
        }

        // Step 5: IPN (Подоходный налог)
        BigDecimal ipnAmount = BigDecimal.ZERO;
        if (!isResident) {
            // Non-residents: flat 20% (Art. 655 Tax Code)
            ipnAmount = taxableIncome.multiply(BigDecimal.valueOf(0.20)).setScale(SCALE, ROUNDING);
        } else {
            ipnAmount = taxableIncome.multiply(ipnRate).setScale(SCALE, ROUNDING);
        }

        // Step 6: Net salary
        BigDecimal totalDeductions = opvAmount.add(oopvAmount).add(ipnAmount).add(otherDeductions);
        BigDecimal netSalary = earnedSalary.subtract(totalDeductions).add(allowances);
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            netSalary = BigDecimal.ZERO;
        }

        // Step 7: SO - employer contribution (not deducted from employee)
        BigDecimal soBase = earnedSalary.subtract(opvAmount);
        if (soBase.compareTo(BigDecimal.ZERO) < 0) soBase = BigDecimal.ZERO;
        BigDecimal soAmount = soBase.multiply(soRate).setScale(SCALE, ROUNDING);

        // Step 8: SN = earnedSalary * 9.5% - SO (employer pays, minimum 1 MRP)
        BigDecimal snAmount = earnedSalary.multiply(snRate).subtract(soAmount).setScale(SCALE, ROUNDING);
        if (snAmount.compareTo(BigDecimal.ZERO) < 0) snAmount = BigDecimal.ZERO;

        log.debug("Payroll for {} {}: gross={}, earned={}, opv={}, ipn={}, net={}",
                employee.getFirstName(), employee.getLastName(),
                grossSalary, earnedSalary, opvAmount, ipnAmount, netSalary);

        return PayrollCalculationResult.builder()
                .grossSalary(grossSalary)
                .workedDays(workedDays)
                .totalWorkingDays(totalDays)
                .earnedSalary(earnedSalary)
                .allowances(allowances)
                .deductions(otherDeductions)
                .opvAmount(opvAmount)
                .oopvAmount(oopvAmount)
                .taxableIncome(taxableIncome)
                .ipnAmount(ipnAmount)
                .soAmount(soAmount)
                .snAmount(snAmount)
                .totalDeductions(totalDeductions)
                .netSalary(netSalary)
                .isResident(isResident)
                .hasDisability(hasDisability)
                .mrpUsed(mrp)
                .calculationYear(year)
                .calculationMonth(month)
                .build();
    }

    private BigDecimal calculateEarnedSalary(BigDecimal grossSalary, int worked, int total) {
        if (worked == total) return grossSalary;
        return grossSalary
                .multiply(BigDecimal.valueOf(worked))
                .divide(BigDecimal.valueOf(total), SCALE, ROUNDING);
    }

    private BigDecimal calculateOpv(BigDecimal earnedSalary) {
        BigDecimal opv = earnedSalary.multiply(opvRate).setScale(SCALE, ROUNDING);
        BigDecimal cap = BigDecimal.valueOf((long) mrp * OPV_CAP_MRP);
        return opv.min(cap);
    }
}
