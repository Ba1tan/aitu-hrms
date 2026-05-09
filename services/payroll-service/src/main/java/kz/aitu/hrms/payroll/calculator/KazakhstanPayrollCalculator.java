package kz.aitu.hrms.payroll.calculator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Kazakhstan payroll calculator — Tax Code (Закон РК № 214-VIII от 18.07.2025).
 *
 * Calculation order:
 *   Step 1: earned = gross × (worked / total)            (prorate for partial month)
 *   Step 2: OPV    = earned × 10%, capped at 50×МЗП      (skip if pensioner)
 *   Step 3: ВОСМС  = earned × 2%,  capped at 20×МЗП
 *   Step 4: standardDeduction = 30×МРП (residents) + disability bonus
 *           — group 3      → +882×МРП
 *           — group 1 or 2 → +5000×МРП
 *   Step 5: taxable = earned − OPV − ВОСМС − standardDeduction (floor 0)
 *   Step 6: IPN    = taxable × 10% (resident) or × 20% (non-resident)
 *   Step 7: net    = earned − OPV − ВОСМС − IPN + allowances − otherDeductions
 *
 * Employer obligations (NOT deducted, shown on payslip):
 *   Step 8:  SO   = (earned − OPV) × 5%
 *   Step 9:  SN   = earned × 6%   (fixed; no longer reduced by SO since 2025)
 *   Step 10: ОПВР = earned × 3.5%
 *
 * All money is {@link BigDecimal} — never double/float.
 */
@Slf4j
@Component
public class KazakhstanPayrollCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** Tax Code Art. 404: deduction in МРП for disability groups 1/2. */
    private static final int DISABILITY_GROUP_12_MRP = 5000;
    /** Tax Code Art. 404: deduction in МРП for disability group 3. */
    private static final int DISABILITY_GROUP_3_MRP = 882;

    @Value("${app.payroll.mrp}")
    private int mrp;

    @Value("${app.payroll.min-wage}")
    private int minWage;

    @Value("${app.payroll.standard-deduction-mrp}")
    private int standardDeductionMrp;

    @Value("${app.payroll.opv-cap-mzp}")
    private int opvCapMzp;

    @Value("${app.payroll.opv-rate}")
    private BigDecimal opvRate;

    @Value("${app.payroll.vosms-rate}")
    private BigDecimal vosmsRate;

    @Value("${app.payroll.ipn-rate}")
    private BigDecimal ipnRate;

    @Value("${app.payroll.ipn-rate-non-resident}")
    private BigDecimal ipnRateNonResident;

    @Value("${app.payroll.so-rate}")
    private BigDecimal soRate;

    @Value("${app.payroll.sn-rate}")
    private BigDecimal snRate;

    @Value("${app.payroll.opvr-rate}")
    private BigDecimal opvrRate;

    public PayrollCalculationResult calculate(
            EmployeePayrollProfile employee,
            int workedDays,
            int totalDays,
            BigDecimal allowances,
            BigDecimal otherDeductions,
            String yearStr,
            String monthStr) {

        BigDecimal gross = nz(employee.getBaseSalary());
        BigDecimal allow = nz(allowances);
        BigDecimal otherDed = nz(otherDeductions);

        // Step 1
        BigDecimal earned = prorate(gross, workedDays, totalDays);

        // Step 2 — OPV (skip pensioners)
        BigDecimal opv = employee.isPensioner()
                ? BigDecimal.ZERO
                : capAt(earned.multiply(opvRate).setScale(SCALE, ROUNDING),
                        BigDecimal.valueOf((long) minWage * opvCapMzp));

        // Step 3 — ВОСМС (cap = 20×МЗП × rate)
        BigDecimal vosmsCap = BigDecimal.valueOf((long) minWage * 20)
                .multiply(vosmsRate)
                .setScale(SCALE, ROUNDING);
        BigDecimal vosms = capAt(
                earned.multiply(vosmsRate).setScale(SCALE, ROUNDING), vosmsCap);

        // Step 4 — standard + disability deduction (residents only)
        BigDecimal standardDeduction = BigDecimal.ZERO;
        if (employee.isResident()) {
            standardDeduction = BigDecimal.valueOf((long) mrp * standardDeductionMrp);
            int disabilityMrp = disabilityMrpFor(employee);
            if (disabilityMrp > 0) {
                standardDeduction = standardDeduction.add(
                        BigDecimal.valueOf((long) mrp * disabilityMrp));
            }
        }

        // Step 5 — taxable (floor 0)
        BigDecimal taxable = earned.subtract(opv).subtract(vosms).subtract(standardDeduction);
        if (taxable.signum() < 0) {
            taxable = BigDecimal.ZERO;
        }

        // Step 6 — IPN
        BigDecimal effectiveIpnRate = employee.isResident() ? ipnRate : ipnRateNonResident;
        BigDecimal ipn = taxable.multiply(effectiveIpnRate).setScale(SCALE, ROUNDING);

        // Step 7 — net (floor 0)
        BigDecimal oopv = BigDecimal.ZERO;
        BigDecimal totalDeductions = opv.add(vosms).add(oopv).add(ipn).add(otherDed);
        BigDecimal net = earned.subtract(totalDeductions).add(allow);
        if (net.signum() < 0) {
            net = BigDecimal.ZERO;
        }

        // Step 8 — SO 5% on (earned − OPV), floor 0
        BigDecimal soBase = earned.subtract(opv);
        if (soBase.signum() < 0) soBase = BigDecimal.ZERO;
        BigDecimal so = soBase.multiply(soRate).setScale(SCALE, ROUNDING);

        // Step 9 — SN 6% (fixed)
        BigDecimal sn = earned.multiply(snRate).setScale(SCALE, ROUNDING);

        // Step 10 — ОПВР 3.5%
        BigDecimal opvr = earned.multiply(opvrRate).setScale(SCALE, ROUNDING);

        log.debug("Payroll {} {}: gross={}, earned={}, opv={}, vosms={}, taxable={}, ipn={}, net={}",
                employee.getEmployeeNumber(), monthStr,
                gross, earned, opv, vosms, taxable, ipn, net);

        return PayrollCalculationResult.builder()
                .grossSalary(gross)
                .workedDays(workedDays)
                .totalWorkingDays(totalDays)
                .earnedSalary(earned)
                .allowances(allow)
                .deductions(otherDed)
                .opvAmount(opv)
                .vosmsAmount(vosms)
                .oopvAmount(oopv)
                .taxableIncome(taxable)
                .ipnAmount(ipn)
                .soAmount(so)
                .snAmount(sn)
                .opvrAmount(opvr)
                .totalDeductions(totalDeductions)
                .netSalary(net)
                .resident(employee.isResident())
                .disability(employee.isDisabled())
                .mrpUsed(mrp)
                .calculationYear(yearStr)
                .calculationMonth(monthStr)
                .build();
    }

    private static int disabilityMrpFor(EmployeePayrollProfile e) {
        if (!e.isDisabled() || e.getDisabilityGroup() == null) return 0;
        return switch (e.getDisabilityGroup()) {
            case GROUP_1, GROUP_2 -> DISABILITY_GROUP_12_MRP;
            case GROUP_3          -> DISABILITY_GROUP_3_MRP;
            case NONE             -> 0;
        };
    }

    private static BigDecimal prorate(BigDecimal gross, int worked, int total) {
        if (total <= 0) return BigDecimal.ZERO;
        if (worked >= total) return gross.setScale(SCALE, ROUNDING);
        return gross.multiply(BigDecimal.valueOf(worked))
                .divide(BigDecimal.valueOf(total), SCALE, ROUNDING);
    }

    private static BigDecimal capAt(BigDecimal value, BigDecimal cap) {
        return value.compareTo(cap) > 0 ? cap : value;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}