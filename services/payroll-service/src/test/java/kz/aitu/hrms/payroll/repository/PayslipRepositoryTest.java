package kz.aitu.hrms.payroll.repository;

import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaTestConfig.class)
class PayslipRepositoryTest {

    @Autowired private PayslipRepository payslipRepo;
    @Autowired private PayrollPeriodRepository periodRepo;

    @Test
    void search_filtersByStatus() {
        PayrollPeriod period = persistPeriod(2026, 3);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        persistPayslip(period, alice, "Alice", "EMP-1", PayslipStatus.DRAFT);
        persistPayslip(period, bob,   "Bob",   "EMP-2", PayslipStatus.FLAGGED);

        Page<Payslip> drafts = payslipRepo.search(period.getId(), PayslipStatus.DRAFT, null, PageRequest.of(0, 10));
        assertThat(drafts.getTotalElements()).isEqualTo(1);
        assertThat(drafts.getContent().get(0).getEmployeeName()).isEqualTo("Alice");

        Page<Payslip> flagged = payslipRepo.search(period.getId(), PayslipStatus.FLAGGED, null, PageRequest.of(0, 10));
        assertThat(flagged.getTotalElements()).isEqualTo(1);
        assertThat(flagged.getContent().get(0).getEmployeeName()).isEqualTo("Bob");
    }

    @Test
    void search_filtersByName_caseInsensitive() {
        PayrollPeriod period = persistPeriod(2026, 3);
        persistPayslip(period, UUID.randomUUID(), "Иванов Иван", "EMP-100", PayslipStatus.DRAFT);
        persistPayslip(period, UUID.randomUUID(), "Петров Пётр", "EMP-101", PayslipStatus.DRAFT);

        Page<Payslip> result = payslipRepo.search(period.getId(), null, "иванов", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmployeeNumber()).isEqualTo("EMP-100");
    }

    @Test
    void search_filtersByEmployeeNumber() {
        PayrollPeriod period = persistPeriod(2026, 3);
        persistPayslip(period, UUID.randomUUID(), "Алиса", "EMP-077", PayslipStatus.DRAFT);
        persistPayslip(period, UUID.randomUUID(), "Боб",   "EMP-203", PayslipStatus.DRAFT);

        Page<Payslip> result = payslipRepo.search(period.getId(), null, "077", PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void existsByPeriodAndEmployee_isTrueAfterPersist() {
        PayrollPeriod period = persistPeriod(2026, 4);
        UUID emp = UUID.randomUUID();
        assertThat(payslipRepo.existsByPeriodIdAndEmployeeIdAndDeletedFalse(period.getId(), emp)).isFalse();

        persistPayslip(period, emp, "Эмма", "EMP-9", PayslipStatus.DRAFT);
        assertThat(payslipRepo.existsByPeriodIdAndEmployeeIdAndDeletedFalse(period.getId(), emp)).isTrue();
    }

    @Test
    void getYearToDate_aggregatesAcrossMonths() {
        UUID empId = UUID.randomUUID();
        PayrollPeriod jan = persistPeriod(2026, 1);
        PayrollPeriod feb = persistPeriod(2026, 2);
        PayrollPeriod marOtherYear = persistPeriod(2025, 3);

        persistPayslipWithAmounts(jan, empId, new BigDecimal("300000"), new BigDecimal("250575.00"),
                new BigDecimal("30000"), new BigDecimal("13425.00"));
        persistPayslipWithAmounts(feb, empId, new BigDecimal("300000"), new BigDecimal("250575.00"),
                new BigDecimal("30000"), new BigDecimal("13425.00"));
        // Different year — must be excluded
        persistPayslipWithAmounts(marOtherYear, empId, new BigDecimal("999999"), new BigDecimal("999999"),
                new BigDecimal("99999"), new BigDecimal("99999"));

        List<Object[]> rows = payslipRepo.getYearToDate(empId, 2026);
        assertThat(rows).hasSize(1);
        Object[] r = rows.get(0);
        assertThat(((BigDecimal) r[0])).isEqualByComparingTo("600000");      // gross
        assertThat(((BigDecimal) r[2])).isEqualByComparingTo("501150.00");   // net
        assertThat(((BigDecimal) r[3])).isEqualByComparingTo("60000");       // opv
        assertThat(((BigDecimal) r[5])).isEqualByComparingTo("26850.00");    // ipn
        assertThat(((Number) r[9]).longValue()).isEqualTo(2L);               // count
    }

    @Test
    void countByStatus_excludesDeleted() {
        PayrollPeriod period = persistPeriod(2026, 5);
        persistPayslip(period, UUID.randomUUID(), "A", "EMP-A", PayslipStatus.FLAGGED);
        Payslip dropped = persistPayslip(period, UUID.randomUUID(), "B", "EMP-B", PayslipStatus.FLAGGED);
        dropped.setDeleted(true);
        payslipRepo.save(dropped);

        long count = payslipRepo.countByPeriodIdAndStatusAndDeletedFalse(period.getId(), PayslipStatus.FLAGGED);
        assertThat(count).isEqualTo(1L);
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private PayrollPeriod persistPeriod(int year, int month) {
        return periodRepo.save(PayrollPeriod.builder()
                .year(year).month(month).workingDays(22)
                .startDate(LocalDate.of(year, month, 1))
                .endDate(LocalDate.of(year, month, 1).withDayOfMonth(
                        LocalDate.of(year, month, 1).lengthOfMonth()))
                .status(PayrollPeriodStatus.PROCESSING)
                .build());
    }

    private Payslip persistPayslip(PayrollPeriod period, UUID employeeId,
                                   String name, String number, PayslipStatus status) {
        return payslipRepo.save(Payslip.builder()
                .period(period)
                .employeeId(employeeId)
                .employeeName(name)
                .employeeNumber(number)
                .workedDays(22).totalWorkingDays(22)
                .grossSalary(new BigDecimal("100000"))
                .earnedSalary(new BigDecimal("100000"))
                .opvAmount(new BigDecimal("10000"))
                .vosmsAmount(new BigDecimal("2000"))
                .taxableIncome(new BigDecimal("58000"))
                .ipnAmount(new BigDecimal("5800"))
                .totalDeductions(new BigDecimal("17800"))
                .netSalary(new BigDecimal("82200"))
                .soAmount(new BigDecimal("4500"))
                .snAmount(new BigDecimal("6000"))
                .opvrAmount(new BigDecimal("3500"))
                .mrpUsed(4325)
                .status(status)
                .build());
    }

    private void persistPayslipWithAmounts(PayrollPeriod period, UUID empId,
                                           BigDecimal gross, BigDecimal net,
                                           BigDecimal opv, BigDecimal ipn) {
        payslipRepo.save(Payslip.builder()
                .period(period).employeeId(empId)
                .workedDays(22).totalWorkingDays(22)
                .grossSalary(gross).earnedSalary(gross)
                .opvAmount(opv).vosmsAmount(BigDecimal.ZERO)
                .taxableIncome(gross.subtract(opv))
                .ipnAmount(ipn)
                .totalDeductions(opv.add(ipn))
                .netSalary(net)
                .soAmount(BigDecimal.ZERO).snAmount(BigDecimal.ZERO).opvrAmount(BigDecimal.ZERO)
                .mrpUsed(4325)
                .status(PayslipStatus.APPROVED)
                .build());
    }
}