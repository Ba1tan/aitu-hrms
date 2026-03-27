package kz.aitu.hrms.modules.payroll.calculator;

import kz.aitu.hrms.modules.employee.entity.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kazakhstan payroll calculator Tax Code (Закон РК № 214-VIII от 18.07.2025).
 *
 * Calculation order:
 * Step 1: Earned salary (prorate if partial month)
 * Step 2: OPV  = earned × 10%, capped at 50×МЗП          (ОПВ, employee pension)
 * Step 3: ВОСМС = earned × 2%, capped at 20×МЗП          (employee medical insurance)
 * Step 4: Standard deduction = 30×МРП (residents only)
 *         Disability deduction = 882×МРП (group 3) / 5000×МРП (groups 1&2) — stored in hasDisability flag
 * Step 5: Taxable = earned − OPV − ВОСМС − standard_deduction  (floor 0)
 * Step 6: IPN = taxable × 10% (residents) or × 20% (non-residents)
 * Step 7: Net = earned − OPV − ВОСМС − IPN + allowances − otherDeductions
 *
 * Employer obligations (shown on payslip, NOT deducted from employee):
 * Step 8: SO   = (earned − OPV) × 5%     (СО, social contribution)
 * Step 9: SN   = earned × 6%             (СН, fixed rate — no longer reduced by SO)
 * Step 10: OPVR = earned × 3.5%          (ОПВР, employer pension contribution)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KazakhstanPayrollCalculator {

    @Value("${app.payroll.ipn-rate}")
    private BigDecimal ipnRate;

    @Value("${app.payroll.opv-rate}")
    private BigDecimal opvRate;

    @Value("${app.payroll.vosms-rate}")
    private BigDecimal vosmsRate;

    @Value("${app.payroll.so-rate}")
    private BigDecimal soRate;

    @Value("${app.payroll.sn-rate}")
    private BigDecimal snRate;

    @Value("${app.payroll.opvr-rate}")
    private BigDecimal opvrRate;

    @Value("${app.payroll.oopv-rate}")
    private BigDecimal oopvRate;

    @Value("${app.payroll.mrp}")
    private int mrp;

    @Value("${app.payroll.min-wage}")
    private int minWage;

    @Value("${app.payroll.standard-deduction-mrp}")
    private int standardDeductionMrp;

    @Value("${app.payroll.opv-cap-mzp}")
    private int opvCapMzp;

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // Disability deductions per 2026 Tax Code Art. 404
    private static final int DISABILITY_DEDUCTION_MRP = 882;

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

        // Step 2: OPV employee pension 10%, capped at 50×МЗП
        BigDecimal opvAmount = BigDecimal.ZERO;
        if (!isPensioner) {
            opvAmount = calculateOpv(earnedSalary);
        }

        // Step 3: ВОСМС employee medical insurance 2%, capped at 20×МЗП
        BigDecimal vosmsAmount = calculateVosms(earnedSalary);

        // Step 4: Standard deduction (residents only)
        BigDecimal standardDeduction = BigDecimal.ZERO;
        if (isResident) {
            standardDeduction = BigDecimal.valueOf((long) mrp * standardDeductionMrp);
            if (hasDisability) {
                standardDeduction = standardDeduction
                        .add(BigDecimal.valueOf((long) mrp * DISABILITY_DEDUCTION_MRP));
            }
        }

        // Step 5: Taxable income — ВОСМС now subtracted before IPN (2026 change)
        BigDecimal taxableIncome = earnedSalary
                .subtract(opvAmount)
                .subtract(vosmsAmount)
                .subtract(standardDeduction);
        if (taxableIncome.compareTo(BigDecimal.ZERO) < 0) {
            taxableIncome = BigDecimal.ZERO;
        }

        // Step 6: IPN
        BigDecimal ipnAmount;
        if (!isResident) {
            // Non-residents: flat 20%
            ipnAmount = taxableIncome.multiply(BigDecimal.valueOf(0.20)).setScale(SCALE, ROUNDING);
        } else {
            ipnAmount = taxableIncome.multiply(ipnRate).setScale(SCALE, ROUNDING);
        }

        // Step 7: Net salary (ВОСМС also deducted from net)
        BigDecimal oopvAmount = BigDecimal.ZERO; // only for specific professions
        BigDecimal totalDeductions = opvAmount
                .add(vosmsAmount)
                .add(oopvAmount)
                .add(ipnAmount)
                .add(otherDeductions);
        BigDecimal netSalary = earnedSalary.subtract(totalDeductions).add(allowances);
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            netSalary = BigDecimal.ZERO;
        }

        // Step 8: SO employer social contribution 5%
        BigDecimal soBase = earnedSalary.subtract(opvAmount);
        if (soBase.compareTo(BigDecimal.ZERO) < 0) soBase = BigDecimal.ZERO;
        BigDecimal soAmount = soBase.multiply(soRate).setScale(SCALE, ROUNDING);

        // Step 9: SN fixed 6%
        BigDecimal snAmount = earnedSalary.multiply(snRate).setScale(SCALE, ROUNDING);

        // Step 10: OPVR — employer pension 3.5%
        BigDecimal opvrAmount = earnedSalary.multiply(opvrRate).setScale(SCALE, ROUNDING);

        log.debug("Payroll 2026 for {} {}: gross={}, earned={}, opv={}, vosms={}, " +
                        "taxable={}, ipn={}, net={}, so={}, sn={}, opvr={}",
                employee.getFirstName(), employee.getLastName(),
                grossSalary, earnedSalary, opvAmount, vosmsAmount,
                taxableIncome, ipnAmount, netSalary, soAmount, snAmount, opvrAmount);

        return PayrollCalculationResult.builder()
                .grossSalary(grossSalary)
                .workedDays(workedDays)
                .totalWorkingDays(totalDays)
                .earnedSalary(earnedSalary)
                .allowances(allowances)
                .deductions(otherDeductions)
                .opvAmount(opvAmount)
                .vosmsAmount(vosmsAmount)
                .oopvAmount(oopvAmount)
                .taxableIncome(taxableIncome)
                .ipnAmount(ipnAmount)
                .soAmount(soAmount)
                .snAmount(snAmount)
                .opvrAmount(opvrAmount)
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
        // Cap: 50 × МЗП
        BigDecimal cap = BigDecimal.valueOf((long) minWage * opvCapMzp);
        return opv.min(cap);
    }

    private BigDecimal calculateVosms(BigDecimal earnedSalary) {
        BigDecimal vosms = earnedSalary.multiply(vosmsRate).setScale(SCALE, ROUNDING);
        // Cap: 20 × МЗП = 1 700 000 tenge → max VOSMS = 34 000
        BigDecimal cap = BigDecimal.valueOf((long) minWage * 20).multiply(vosmsRate)
                .setScale(SCALE, ROUNDING);
        return vosms.min(cap);
    }
}