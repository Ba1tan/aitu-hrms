package kz.aitu.hrms.payroll.calculator;

import kz.aitu.hrms.common.types.DisabilityGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-unit tests for the Kazakhstan 2026 tax calculator. The calculator is
 * a Spring component because it reads rates from {@code @Value}; here we
 * inject those rates via {@link ReflectionTestUtils} so the math can be
 * exercised without bringing up a context.
 */
class KazakhstanPayrollCalculatorTest {

    private KazakhstanPayrollCalculator calc;

    @BeforeEach
    void setUp() {
        calc = new KazakhstanPayrollCalculator();
        ReflectionTestUtils.setField(calc, "mrp", 4325);
        ReflectionTestUtils.setField(calc, "minWage", 85000);
        ReflectionTestUtils.setField(calc, "standardDeductionMrp", 30);
        ReflectionTestUtils.setField(calc, "opvCapMzp", 50);
        ReflectionTestUtils.setField(calc, "opvRate", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(calc, "vosmsRate", new BigDecimal("0.02"));
        ReflectionTestUtils.setField(calc, "ipnRate", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(calc, "ipnRateNonResident", new BigDecimal("0.20"));
        ReflectionTestUtils.setField(calc, "soRate", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(calc, "snRate", new BigDecimal("0.06"));
        ReflectionTestUtils.setField(calc, "opvrRate", new BigDecimal("0.035"));
    }

    private EmployeePayrollProfile resident(BigDecimal salary) {
        return EmployeePayrollProfile.builder()
                .employeeId(UUID.randomUUID())
                .employeeNumber("EMP-001")
                .fullName("Иванов Иван")
                .baseSalary(salary)
                .resident(true)
                .pensioner(false)
                .disabled(false)
                .disabilityGroup(DisabilityGroup.NONE)
                .build();
    }

    @Nested
    @DisplayName("Resident, full month, no extras")
    class StandardCase {

        @Test
        @DisplayName("300 000 ₸ resident, full month — matches API_CONTRACT example")
        void standardResident() {
            // From docs/API_CONTRACT.md sample payslip
            EmployeePayrollProfile e = resident(new BigDecimal("300000.00"));

            PayrollCalculationResult r = calc.calculate(
                    e, 22, 22,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    "2026", "Март 2026");

            assertThat(r.getEarnedSalary()).isEqualByComparingTo("300000.00");
            assertThat(r.getOpvAmount()).isEqualByComparingTo("30000.00");
            assertThat(r.getVosmsAmount()).isEqualByComparingTo("6000.00");
            // taxable = 300000 - 30000 - 6000 - 30*4325 = 134250
            assertThat(r.getTaxableIncome()).isEqualByComparingTo("134250.00");
            assertThat(r.getIpnAmount()).isEqualByComparingTo("13425.00");
            // net = 300000 - 30000 - 6000 - 13425 = 250575
            assertThat(r.getNetSalary()).isEqualByComparingTo("250575.00");
            // employer
            assertThat(r.getSoAmount()).isEqualByComparingTo("13500.00");   // (300000-30000)*5%
            assertThat(r.getSnAmount()).isEqualByComparingTo("18000.00");   // 300000*6%
            assertThat(r.getOpvrAmount()).isEqualByComparingTo("10500.00"); // 300000*3.5%
            assertThat(r.isResident()).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class Edges {

        @Test
        @DisplayName("Pensioner: OPV is skipped")
        void pensionerSkipsOpv() {
            EmployeePayrollProfile e = EmployeePayrollProfile.builder()
                    .employeeId(UUID.randomUUID())
                    .baseSalary(new BigDecimal("250000"))
                    .resident(true).pensioner(true).disabled(false)
                    .disabilityGroup(DisabilityGroup.NONE)
                    .build();

            PayrollCalculationResult r = calc.calculate(
                    e, 20, 20, BigDecimal.ZERO, BigDecimal.ZERO, "2026", "Март 2026");

            assertThat(r.getOpvAmount()).isEqualByComparingTo("0");
            assertThat(r.getVosmsAmount()).isEqualByComparingTo("5000.00");
        }

        @Test
        @DisplayName("Non-resident pays IPN at 20%")
        void nonResidentIpn() {
            EmployeePayrollProfile e = EmployeePayrollProfile.builder()
                    .employeeId(UUID.randomUUID())
                    .baseSalary(new BigDecimal("500000"))
                    .resident(false).pensioner(false).disabled(false)
                    .disabilityGroup(DisabilityGroup.NONE)
                    .build();

            PayrollCalculationResult r = calc.calculate(
                    e, 22, 22, BigDecimal.ZERO, BigDecimal.ZERO, "2026", "Март 2026");

            // No standardDeduction for non-residents — taxable = 500000-50000-10000 = 440000
            assertThat(r.getTaxableIncome()).isEqualByComparingTo("440000.00");
            // 20% of 440000
            assertThat(r.getIpnAmount()).isEqualByComparingTo("88000.00");
        }

        @Test
        @DisplayName("Disability group 1: extra 5000×МРП deduction → IPN 0 below threshold")
        void disabilityGroup1() {
            EmployeePayrollProfile e = EmployeePayrollProfile.builder()
                    .employeeId(UUID.randomUUID())
                    .baseSalary(new BigDecimal("200000"))
                    .resident(true).pensioner(false).disabled(true)
                    .disabilityGroup(DisabilityGroup.GROUP_1)
                    .build();

            PayrollCalculationResult r = calc.calculate(
                    e, 22, 22, BigDecimal.ZERO, BigDecimal.ZERO, "2026", "Март 2026");

            // Deduction (30+5000) × 4325 = 21,756,250 — vastly exceeds earned, so taxable=0 → IPN=0
            assertThat(r.getTaxableIncome()).isEqualByComparingTo("0");
            assertThat(r.getIpnAmount()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Partial month: 11/22 worked → earned salary halves")
        void prorate() {
            EmployeePayrollProfile e = resident(new BigDecimal("200000"));

            PayrollCalculationResult r = calc.calculate(
                    e, 11, 22, BigDecimal.ZERO, BigDecimal.ZERO, "2026", "Март 2026");

            assertThat(r.getEarnedSalary()).isEqualByComparingTo("100000.00");
            assertThat(r.getOpvAmount()).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("Allowances raise net pay; deductions lower it")
        void allowancesAndDeductions() {
            EmployeePayrollProfile e = resident(new BigDecimal("200000"));

            PayrollCalculationResult base = calc.calculate(
                    e, 22, 22, BigDecimal.ZERO, BigDecimal.ZERO, "2026", "Март 2026");

            PayrollCalculationResult withExtras = calc.calculate(
                    e, 22, 22,
                    new BigDecimal("15000"),  // bonus
                    new BigDecimal("3000"),   // fine
                    "2026", "Март 2026");

            BigDecimal expectedDelta = new BigDecimal("12000.00").setScale(2, RoundingMode.HALF_UP);
            assertThat(withExtras.getNetSalary().subtract(base.getNetSalary()))
                    .isEqualByComparingTo(expectedDelta);
        }

        @Test
        @DisplayName("OPV cap kicks in for very high earners")
        void opvCap() {
            // 50 × 85000 = 4,250,000 → OPV would naively be much higher
            EmployeePayrollProfile e = resident(new BigDecimal("60000000"));

            PayrollCalculationResult r = calc.calculate(
                    e, 22, 22, BigDecimal.ZERO, BigDecimal.ZERO, "2026", "Март 2026");

            // Capped at 50 × МЗП = 4,250,000
            assertThat(r.getOpvAmount()).isEqualByComparingTo("4250000");
        }
    }
}