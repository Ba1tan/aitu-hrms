package kz.aitu.hrms.payroll.repository;

import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistence-layer tests against H2 in PostgreSQL mode. Validates JPA mapping
 * (so a column rename or constraint break is caught here), custom repository
 * queries, and soft-delete semantics.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaTestConfig.class)
class PayrollPeriodRepositoryTest {

    @Autowired private PayrollPeriodRepository repo;

    @Test
    void existsByYearAndMonth_respectsSoftDelete() {
        PayrollPeriod p = save(2026, 3, PayrollPeriodStatus.DRAFT);
        assertThat(repo.existsByYearAndMonthAndDeletedFalse(2026, 3)).isTrue();

        p.setDeleted(true);
        repo.save(p);
        assertThat(repo.existsByYearAndMonthAndDeletedFalse(2026, 3)).isFalse();
    }

    @Test
    void list_orderedByYearMonthDescending() {
        save(2025, 12, PayrollPeriodStatus.PAID);
        save(2026, 1, PayrollPeriodStatus.LOCKED);
        save(2026, 4, PayrollPeriodStatus.DRAFT);
        save(2026, 2, PayrollPeriodStatus.APPROVED);

        Page<PayrollPeriod> page = repo.findAllByDeletedFalseOrderByYearDescMonthDesc(PageRequest.of(0, 10));

        List<String> orderedKey = page.getContent().stream()
                .map(p -> p.getYear() + "-" + String.format("%02d", p.getMonth()))
                .toList();
        assertThat(orderedKey).containsExactly("2026-04", "2026-02", "2026-01", "2025-12");
    }

    @Test
    void findByYearAndMonth_returnsExactMatch() {
        save(2026, 5, PayrollPeriodStatus.DRAFT);
        Optional<PayrollPeriod> found = repo.findByYearAndMonthAndDeletedFalse(2026, 5);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Май 2026");
    }

    @Test
    void findTopByStatusNot_skipsLocked() {
        save(2026, 1, PayrollPeriodStatus.LOCKED);
        save(2026, 2, PayrollPeriodStatus.PAID);
        save(2026, 3, PayrollPeriodStatus.DRAFT);

        Optional<PayrollPeriod> found = repo.findTopByStatusNotAndDeletedFalseOrderByYearDescMonthDesc(
                PayrollPeriodStatus.LOCKED);
        assertThat(found).isPresent();
        assertThat(found.get().getMonth()).isEqualTo(3);
    }

    private PayrollPeriod save(int y, int m, PayrollPeriodStatus s) {
        return repo.save(PayrollPeriod.builder()
                .year(y).month(m).workingDays(22)
                .startDate(LocalDate.of(y, m, 1))
                .endDate(LocalDate.of(y, m, 1).withDayOfMonth(LocalDate.of(y, m, 1).lengthOfMonth()))
                .status(s)
                .build());
    }
}