package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.common.event.PayrollPeriodApprovedEvent;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.payroll.dto.PeriodDtos;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import kz.aitu.hrms.payroll.repository.PayrollPeriodRepository;
import kz.aitu.hrms.payroll.repository.PayslipRepository;
import kz.aitu.hrms.payroll.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollPeriodService {

    private final PayrollPeriodRepository periodRepo;
    private final PayslipRepository payslipRepo;
    private final PayrollMapper mapper;
    private final EventPublisher events;

    @Transactional
    public PeriodDtos.Response create(PeriodDtos.CreateRequest req) {
        if (periodRepo.existsByYearAndMonthAndDeletedFalse(req.getYear(), req.getMonth())) {
            throw new BusinessException(
                    "Payroll period already exists for "
                            + req.getYear() + "-" + String.format("%02d", req.getMonth()));
        }
        LocalDate start = LocalDate.of(req.getYear(), req.getMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        PayrollPeriod p = PayrollPeriod.builder()
                .year(req.getYear())
                .month(req.getMonth())
                .startDate(start)
                .endDate(end)
                .workingDays(req.getWorkingDays())
                .status(PayrollPeriodStatus.DRAFT)
                .build();
        p = periodRepo.save(p);
        log.info("Payroll period created: {}", p.getName());
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public Page<PeriodDtos.Response> list(Pageable pageable) {
        return periodRepo.findAllByDeletedFalseOrderByYearDescMonthDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PeriodDtos.Response detail(UUID periodId) {
        return toResponse(requirePeriod(periodId));
    }

    @Transactional
    public PeriodDtos.Response approve(UUID periodId) {
        PayrollPeriod period = requirePeriod(periodId);
        if (period.getStatus() != PayrollPeriodStatus.COMPLETED
                && period.getStatus() != PayrollPeriodStatus.PROCESSING) {
            throw new BusinessException(
                    "Only COMPLETED periods can be approved (current: " + period.getStatus() + ")");
        }
        long flagged = payslipRepo.countByPeriodIdAndStatusAndDeletedFalse(periodId, PayslipStatus.FLAGGED);
        if (flagged > 0) {
            throw new BusinessException(
                    "Period has " + flagged + " flagged payslip(s). Resolve them via approve-flagged first.");
        }

        long count = payslipRepo.countByPeriodIdAndDeletedFalse(periodId);
        if (count == 0) {
            throw new BusinessException("No payslips found for this period. Generate payslips first.");
        }

        // Move all DRAFT payslips → APPROVED
        List<Payslip> drafts = payslipRepo.findByPeriodIdAndDeletedFalse(periodId).stream()
                .filter(p -> p.getStatus() == PayslipStatus.DRAFT)
                .toList();
        drafts.forEach(p -> p.setStatus(PayslipStatus.APPROVED));
        payslipRepo.saveAll(drafts);

        period.setStatus(PayrollPeriodStatus.APPROVED);
        period.setApprovedBy(CurrentUser.userId());
        period.setApprovedAt(LocalDateTime.now());
        period = periodRepo.save(period);

        BigDecimal totalNet = payslipRepo.sumNetSalaryByPeriod(periodId);
        events.publishPeriodApproved(PayrollPeriodApprovedEvent.builder()
                .periodId(period.getId())
                .year(period.getYear())
                .month(period.getMonth())
                .payslipCount(count)
                .totalNet(totalNet)
                .approvedAt(period.getApprovedAt())
                .approvedBy(period.getApprovedBy())
                .build());

        log.info("Period {} approved by {}", period.getName(), period.getApprovedBy());
        return toResponse(period);
    }

    @Transactional
    public PeriodDtos.Response markPaid(UUID periodId) {
        PayrollPeriod period = requirePeriod(periodId);
        if (period.getStatus() != PayrollPeriodStatus.APPROVED) {
            throw new BusinessException(
                    "Only APPROVED periods can be marked as paid (current: " + period.getStatus() + ")");
        }
        List<Payslip> approved = payslipRepo.findByPeriodIdAndDeletedFalse(periodId).stream()
                .filter(p -> p.getStatus() == PayslipStatus.APPROVED)
                .toList();
        approved.forEach(p -> p.setStatus(PayslipStatus.PAID));
        payslipRepo.saveAll(approved);

        period.setStatus(PayrollPeriodStatus.PAID);
        log.info("Period {} marked as paid", period.getName());
        return toResponse(periodRepo.save(period));
    }

    @Transactional
    public PeriodDtos.Response lock(UUID periodId) {
        PayrollPeriod period = requirePeriod(periodId);
        if (period.getStatus() != PayrollPeriodStatus.PAID) {
            throw new BusinessException(
                    "Only PAID periods can be locked (current: " + period.getStatus() + ")");
        }
        period.setStatus(PayrollPeriodStatus.LOCKED);
        log.info("Period {} locked", period.getName());
        return toResponse(periodRepo.save(period));
    }

    PayrollPeriod requirePeriod(UUID periodId) {
        return periodRepo.findByIdAndDeletedFalse(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollPeriod", periodId));
    }

    PeriodDtos.Response toResponse(PayrollPeriod p) {
        PeriodDtos.Response r = mapper.toPeriodResponse(p);
        attachSummary(r, p.getId());
        return r;
    }

    private void attachSummary(PeriodDtos.Response r, UUID periodId) {
        try {
            List<Object[]> rows = payslipRepo.getPeriodTotals(periodId);
            if (rows == null || rows.isEmpty()) return;
            Object[] row = rows.get(0);
            long count = toLong(row[8]);
            if (count == 0) return;
            r.setSummary(PeriodDtos.Summary.builder()
                    .totalGrossSalary(toBd(row[0]))
                    .totalNetSalary(toBd(row[1]))
                    .totalIpn(toBd(row[2]))
                    .totalOpv(toBd(row[3]))
                    .totalVosms(toBd(row[4]))
                    .totalSo(toBd(row[5]))
                    .totalSn(toBd(row[6]))
                    .totalOpvr(toBd(row[7]))
                    .payslipCount(count)
                    .approvedCount(payslipRepo.countByPeriodIdAndStatusAndDeletedFalse(periodId, PayslipStatus.APPROVED))
                    .flaggedCount(payslipRepo.countByPeriodIdAndStatusAndDeletedFalse(periodId, PayslipStatus.FLAGGED))
                    .build());
        } catch (Exception e) {
            log.debug("Could not load period summary for {}: {}", periodId, e.getMessage());
        }
    }

    private static BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private static long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long l) return l;
        return Long.parseLong(o.toString());
    }
}