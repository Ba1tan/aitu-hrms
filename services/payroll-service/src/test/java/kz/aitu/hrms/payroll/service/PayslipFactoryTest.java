package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.payroll.calculator.EmployeePayrollProfile;
import kz.aitu.hrms.payroll.calculator.KazakhstanPayrollCalculator;
import kz.aitu.hrms.payroll.client.AttendanceClient;
import kz.aitu.hrms.payroll.entity.AdditionCategory;
import kz.aitu.hrms.payroll.entity.AdditionType;
import kz.aitu.hrms.payroll.entity.PayrollAddition;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.repository.PayrollAdditionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayslipFactoryTest {

    @Mock private AttendanceClient attendanceClient;
    @Mock private PayrollAdditionRepository additionRepo;

    private PayslipFactory factory;

    @BeforeEach
    void setUp() {
        KazakhstanPayrollCalculator calculator = new KazakhstanPayrollCalculator();
        ReflectionTestUtils.setField(calculator, "mrp", 4325);
        ReflectionTestUtils.setField(calculator, "minWage", 85000);
        ReflectionTestUtils.setField(calculator, "standardDeductionMrp", 30);
        ReflectionTestUtils.setField(calculator, "opvCapMzp", 50);
        ReflectionTestUtils.setField(calculator, "opvRate", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(calculator, "vosmsRate", new BigDecimal("0.02"));
        ReflectionTestUtils.setField(calculator, "ipnRate", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(calculator, "ipnRateNonResident", new BigDecimal("0.20"));
        ReflectionTestUtils.setField(calculator, "soRate", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(calculator, "snRate", new BigDecimal("0.06"));
        ReflectionTestUtils.setField(calculator, "opvrRate", new BigDecimal("0.035"));

        factory = new PayslipFactory(calculator, attendanceClient, additionRepo);
        ReflectionTestUtils.setField(factory, "anomalyFlagThreshold", new BigDecimal("0.65"));
    }

    @Test
    void build_appliesBonusesAsAllowancesAndFinesAsDeductions() {
        UUID employeeId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();

        PayrollPeriod period = PayrollPeriod.builder()
                .year(2026).month(3).workingDays(22)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .status(PayrollPeriodStatus.PROCESSING)
                .build();
        period.setId(periodId);

        EmployeePayrollProfile profile = EmployeePayrollProfile.builder()
                .employeeId(employeeId)
                .employeeNumber("EMP-1")
                .iin("000000000000")
                .fullName("Тестов Тест")
                .baseSalary(new BigDecimal("200000"))
                .resident(true).pensioner(false).disabled(false)
                .disabilityGroup(kz.aitu.hrms.common.types.DisabilityGroup.NONE)
                .build();

        PayrollAddition bonus = PayrollAddition.builder()
                .employeeId(employeeId).periodId(periodId)
                .type(AdditionType.BONUS).category(AdditionCategory.BONUS_PERFORMANCE)
                .amount(new BigDecimal("20000")).taxable(true)
                .build();
        PayrollAddition fine = PayrollAddition.builder()
                .employeeId(employeeId).periodId(periodId)
                .type(AdditionType.DEDUCTION).category(AdditionCategory.FINE)
                .amount(new BigDecimal("5000")).taxable(false)
                .build();

        when(additionRepo.findByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, employeeId))
                .thenReturn(List.of(bonus, fine));
        // attendance unavailable → factory falls back to full month
        when(attendanceClient.summary(any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("attendance down"));

        Payslip slip = factory.build(profile, period);

        assertThat(slip.getEmployeeId()).isEqualTo(employeeId);
        assertThat(slip.getEmployeeIin()).isEqualTo("000000000000");
        assertThat(slip.getAllowances()).isEqualByComparingTo("20000");
        assertThat(slip.getOtherDeductions()).isEqualByComparingTo("5000");
        // gross unchanged; net should reflect +20000 −5000 vs. base
        assertThat(slip.getGrossSalary()).isEqualByComparingTo("200000");
        assertThat(slip.getEarnedSalary()).isEqualByComparingTo("200000");
        assertThat(slip.getWorkedDays()).isEqualTo(22);
    }
}