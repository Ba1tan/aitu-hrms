package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.payroll.dto.PeriodDtos;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import kz.aitu.hrms.payroll.repository.PayrollPeriodRepository;
import kz.aitu.hrms.payroll.repository.PayslipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollPeriodServiceTest {

    @Mock private PayrollPeriodRepository periodRepo;
    @Mock private PayslipRepository payslipRepo;
    @Mock private PayrollMapper mapper;
    @Mock private EventPublisher events;

    @InjectMocks private PayrollPeriodService service;

    @BeforeEach
    void setUp() {
        // Default: a benign mapper that returns a populated response
        lenient().when(mapper.toPeriodResponse(any())).thenAnswer(inv -> {
            PayrollPeriod p = inv.getArgument(0);
            return PeriodDtos.Response.builder()
                    .id(p.getId())
                    .year(p.getYear())
                    .month(p.getMonth())
                    .status(p.getStatus())
                    .build();
        });
        lenient().when(payslipRepo.getPeriodTotals(any())).thenReturn(List.of());
    }

    @Test
    void create_rejectsDuplicatePeriod() {
        PeriodDtos.CreateRequest req = new PeriodDtos.CreateRequest();
        req.setYear(2026); req.setMonth(3); req.setWorkingDays(22);
        when(periodRepo.existsByYearAndMonthAndDeletedFalse(2026, 3)).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void create_persistsNewPeriod() {
        PeriodDtos.CreateRequest req = new PeriodDtos.CreateRequest();
        req.setYear(2026); req.setMonth(4); req.setWorkingDays(22);
        when(periodRepo.existsByYearAndMonthAndDeletedFalse(2026, 4)).thenReturn(false);
        when(periodRepo.save(any(PayrollPeriod.class))).thenAnswer(inv -> {
            PayrollPeriod p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PeriodDtos.Response resp = service.create(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getStatus()).isEqualTo(PayrollPeriodStatus.DRAFT);
        assertThat(resp.getYear()).isEqualTo(2026);
        assertThat(resp.getMonth()).isEqualTo(4);
    }

    @Test
    void approve_rejectsWhenFlaggedPayslipsExist() {
        UUID id = UUID.randomUUID();
        PayrollPeriod period = newPeriod(id, PayrollPeriodStatus.COMPLETED);
        when(periodRepo.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(period));
        when(payslipRepo.countByPeriodIdAndStatusAndDeletedFalse(id, PayslipStatus.FLAGGED)).thenReturn(2L);

        assertThatThrownBy(() -> service.approve(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("flagged");
    }

    @Test
    void approve_rejectsWhenWrongStatus() {
        UUID id = UUID.randomUUID();
        PayrollPeriod period = newPeriod(id, PayrollPeriodStatus.PAID);
        when(periodRepo.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.approve(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    void markPaid_rejectsWhenNotApproved() {
        UUID id = UUID.randomUUID();
        PayrollPeriod period = newPeriod(id, PayrollPeriodStatus.COMPLETED);
        when(periodRepo.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.markPaid(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    void lock_rejectsWhenNotPaid() {
        UUID id = UUID.randomUUID();
        PayrollPeriod period = newPeriod(id, PayrollPeriodStatus.APPROVED);
        when(periodRepo.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.lock(id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PAID");
    }

    private static PayrollPeriod newPeriod(UUID id, PayrollPeriodStatus status) {
        PayrollPeriod p = PayrollPeriod.builder()
                .year(2026).month(3).workingDays(22)
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 3, 31))
                .status(status)
                .build();
        p.setId(id);
        return p;
    }
}