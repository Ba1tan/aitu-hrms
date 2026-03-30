package kz.aitu.hrms.modules.payroll.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.modules.employee.entity.Employee;
import kz.aitu.hrms.modules.employee.service.EmployeeService;
import kz.aitu.hrms.modules.payroll.calculator.KazakhstanPayrollCalculator;
import kz.aitu.hrms.modules.payroll.calculator.PayrollCalculationResult;
import kz.aitu.hrms.modules.payroll.dto.PayrollDtos;
import kz.aitu.hrms.modules.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.modules.payroll.entity.Payslip;
import kz.aitu.hrms.modules.payroll.enums.PayrollPeriodStatus;
import kz.aitu.hrms.modules.payroll.enums.PayslipStatus;
import kz.aitu.hrms.modules.payroll.repository.PayrollPeriodRepository;
import kz.aitu.hrms.modules.payroll.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollPeriodRepository periodRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeService employeeService;
    private final KazakhstanPayrollCalculator calculator;

    // =========================================================================
    // PERIOD MANAGEMENT
    // =========================================================================

    @Override
    @Transactional
    public PayrollDtos.PeriodResponse createPeriod(PayrollDtos.CreatePeriodRequest req) {
        if (periodRepository.existsByYearAndMonthAndDeletedFalse(req.getYear(), req.getMonth())) {
            throw new BusinessException(
                    "Payroll period already exists for " + req.getYear() + "-" + String.format("%02d", req.getMonth()));
        }

        LocalDate start = LocalDate.of(req.getYear(), req.getMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        PayrollPeriod period = PayrollPeriod.builder()
                .year(req.getYear())
                .month(req.getMonth())
                .startDate(start)
                .endDate(end)
                .workingDays(req.getWorkingDays())
                .status(PayrollPeriodStatus.DRAFT)
                .build();

        PayrollPeriod saved = periodRepository.save(period);
        log.info("Payroll period created: {}", saved.getName());
        return toPeriodResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollDtos.PeriodResponse> getPeriods(Pageable pageable) {
        return periodRepository.findAllByDeletedFalseOrderByYearDescMonthDesc(pageable)
                .map(this::toPeriodResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDtos.PeriodResponse getPeriod(UUID periodId) {
        return toPeriodResponse(findPeriodOrThrow(periodId));
    }

    // =========================================================================
    // PAYSLIP GENERATION
    // =========================================================================

    @Override
    @Transactional
    public PayrollDtos.GeneratePayslipsResponse generatePayslips(
            UUID periodId, PayrollDtos.GeneratePayslipsRequest req) {

        PayrollPeriod period = findPeriodOrThrow(periodId);

        if (period.getStatus() == PayrollPeriodStatus.LOCKED) {
            throw new BusinessException("Cannot generate payslips for a locked period");
        }
        if (period.getStatus() == PayrollPeriodStatus.PAID) {
            throw new BusinessException("Cannot modify a paid period");
        }

        // Determine target employees
        List<Employee> employees = employeeService.findActiveEmployees();
        if (req != null && req.getEmployeeIds() != null && !req.getEmployeeIds().isEmpty()) {
            Set<UUID> filter = Set.copyOf(req.getEmployeeIds());
            employees = employees.stream()
                    .filter(e -> filter.contains(e.getId()))
                    .collect(Collectors.toList());
        }

        int generated = 0, skipped = 0, errors = 0;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        List<String> errorDetails = new ArrayList<>();

        String yearStr = String.valueOf(period.getYear());
        String monthStr = period.getName();

        for (Employee emp : employees) {
            try {
                // Skip if payslip already exists for this employee+period
                if (payslipRepository.existsByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, emp.getId())) {
                    skipped++;
                    continue;
                }

                PayrollCalculationResult result = calculator.calculate(
                        emp,
                        period.getWorkingDays(),   // assume full month for MVP
                        period.getWorkingDays(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        yearStr,
                        monthStr
                );

                Payslip payslip = Payslip.builder()
                        .period(period)
                        .employee(emp)
                        .workedDays(result.getWorkedDays())
                        .totalWorkingDays(result.getTotalWorkingDays())
                        .grossSalary(result.getGrossSalary())
                        .earnedSalary(result.getEarnedSalary())
                        .allowances(result.getAllowances())
                        .opvAmount(result.getOpvAmount())
                        .oopvAmount(result.getOopvAmount())
                        .taxableIncome(result.getTaxableIncome())
                        .ipnAmount(result.getIpnAmount())
                        .otherDeductions(result.getDeductions())
                        .totalDeductions(result.getTotalDeductions())
                        .netSalary(result.getNetSalary())
                        .soAmount(result.getSoAmount())
                        .snAmount(result.getSnAmount())
                        .mrpUsed(result.getMrpUsed())
                        .resident(result.isResident())
                        .status(PayslipStatus.DRAFT)
                        .build();

                payslipRepository.save(payslip);
                totalGross = totalGross.add(result.getGrossSalary());
                totalNet = totalNet.add(result.getNetSalary());
                generated++;

            } catch (Exception ex) {
                errors++;
                String msg = String.format("Error processing %s (%s): %s",
                        emp.getFullName(), emp.getEmployeeNumber(), ex.getMessage());
                errorDetails.add(msg);
                log.error("Payslip generation error for employee {}: {}", emp.getEmployeeNumber(), ex.getMessage());
            }
        }

        // Transition period to PROCESSING if it was DRAFT
        if (period.getStatus() == PayrollPeriodStatus.DRAFT && generated > 0) {
            period.setStatus(PayrollPeriodStatus.PROCESSING);
            UUID currentUserId = getCurrentUserId();
            period.setProcessedBy(currentUserId);
            period.setProcessedAt(LocalDateTime.now());
            periodRepository.save(period);
        }

        log.info("Payslip generation for {}: generated={}, skipped={}, errors={}",
                period.getName(), generated, skipped, errors);

        PayrollDtos.GeneratePayslipsResponse resp = new PayrollDtos.GeneratePayslipsResponse();
        resp.setGenerated(generated);
        resp.setSkipped(skipped);
        resp.setErrors(errors);
        resp.setTotalGrossPayout(totalGross);
        resp.setTotalNetPayout(totalNet);
        resp.setErrorDetails(errorDetails);
        return resp;
    }

    // =========================================================================
    // PAYSLIP MANAGEMENT
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollDtos.PayslipResponse> getPayslipsByPeriod(UUID periodId, Pageable pageable) {
        findPeriodOrThrow(periodId); // validate period exists
        return payslipRepository.findByPeriodIdAndDeletedFalse(periodId, pageable)
                .map(this::toPayslipResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDtos.PayslipResponse getPayslip(UUID payslipId) {
        return toPayslipResponse(findPayslipOrThrow(payslipId));
    }

    @Override
    @Transactional
    public PayrollDtos.PayslipResponse adjustPayslip(UUID payslipId, PayrollDtos.AdjustPayslipRequest req) {
        Payslip payslip = findPayslipOrThrow(payslipId);

        if (payslip.getStatus() != PayslipStatus.DRAFT) {
            throw new BusinessException("Only DRAFT payslips can be adjusted. Current status: " + payslip.getStatus());
        }
        if (payslip.getPeriod().getStatus() == PayrollPeriodStatus.LOCKED) {
            throw new BusinessException("Cannot adjust payslip in a locked period");
        }

        // Recalculate with new adjustments
        int workedDays = req.getWorkedDays() != null ? req.getWorkedDays() : payslip.getWorkedDays();
        BigDecimal allowances = req.getAllowances() != null ? req.getAllowances() : payslip.getAllowances();
        BigDecimal deductions = req.getOtherDeductions() != null ? req.getOtherDeductions() : payslip.getOtherDeductions();

        PayrollCalculationResult result = calculator.calculate(
                payslip.getEmployee(),
                workedDays,
                payslip.getTotalWorkingDays(),
                allowances,
                deductions,
                String.valueOf(payslip.getPeriod().getYear()),
                payslip.getPeriod().getName()
        );

        payslip.setWorkedDays(result.getWorkedDays());
        payslip.setEarnedSalary(result.getEarnedSalary());
        payslip.setAllowances(result.getAllowances());
        payslip.setOpvAmount(result.getOpvAmount());
        payslip.setOopvAmount(result.getOopvAmount());
        payslip.setTaxableIncome(result.getTaxableIncome());
        payslip.setIpnAmount(result.getIpnAmount());
        payslip.setOtherDeductions(result.getDeductions());
        payslip.setTotalDeductions(result.getTotalDeductions());
        payslip.setNetSalary(result.getNetSalary());
        payslip.setSoAmount(result.getSoAmount());
        payslip.setSnAmount(result.getSnAmount());

        return toPayslipResponse(payslipRepository.save(payslip));
    }

    // =========================================================================
    // STATUS TRANSITIONS
    // =========================================================================

    @Override
    @Transactional
    public PayrollDtos.PeriodResponse approvePeriod(UUID periodId) {
        PayrollPeriod period = findPeriodOrThrow(periodId);

        if (period.getStatus() != PayrollPeriodStatus.PROCESSING) {
            throw new BusinessException("Only PROCESSING periods can be approved. Current: " + period.getStatus());
        }
        long total = payslipRepository.countByPeriodIdAndStatusAndDeletedFalse(periodId, PayslipStatus.DRAFT)
                   + payslipRepository.countByPeriodIdAndStatusAndDeletedFalse(periodId, PayslipStatus.APPROVED);
        if (total == 0) {
            throw new BusinessException("No payslips found for this period. Generate payslips first.");
        }

        // Approve all DRAFT payslips in this period
        List<Payslip> drafts = payslipRepository.findByPeriodIdAndDeletedFalse(periodId).stream()
                .filter(p -> p.getStatus() == PayslipStatus.DRAFT)
                .collect(Collectors.toList());
        drafts.forEach(p -> p.setStatus(PayslipStatus.APPROVED));
        payslipRepository.saveAll(drafts);

        period.setStatus(PayrollPeriodStatus.APPROVED);
        period.setApprovedBy(getCurrentUserId());
        period.setApprovedAt(LocalDateTime.now());
        log.info("Payroll period approved: {}", period.getName());
        return toPeriodResponse(periodRepository.save(period));
    }

    @Override
    @Transactional
    public PayrollDtos.PeriodResponse markPeriodPaid(UUID periodId) {
        PayrollPeriod period = findPeriodOrThrow(periodId);
        if (period.getStatus() != PayrollPeriodStatus.APPROVED) {
            throw new BusinessException("Only APPROVED periods can be marked as paid. Current: " + period.getStatus());
        }

        // Mark all approved payslips as paid
        List<Payslip> approved = payslipRepository.findByPeriodIdAndDeletedFalse(periodId).stream()
                .filter(p -> p.getStatus() == PayslipStatus.APPROVED)
                .collect(Collectors.toList());
        approved.forEach(p -> p.setStatus(PayslipStatus.PAID));
        payslipRepository.saveAll(approved);

        period.setStatus(PayrollPeriodStatus.PAID);
        log.info("Payroll period marked as paid: {}", period.getName());
        return toPeriodResponse(periodRepository.save(period));
    }

    @Override
    @Transactional
    public PayrollDtos.PeriodResponse lockPeriod(UUID periodId) {
        PayrollPeriod period = findPeriodOrThrow(periodId);
        if (period.getStatus() != PayrollPeriodStatus.PAID) {
            throw new BusinessException("Only PAID periods can be locked. Current: " + period.getStatus());
        }
        period.setStatus(PayrollPeriodStatus.LOCKED);
        log.info("Payroll period locked: {}", period.getName());
        return toPeriodResponse(periodRepository.save(period));
    }

    // =========================================================================
    // EMPLOYEE SELF-SERVICE
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollDtos.PayslipResponse> getMyPayslips(UUID employeeId, Pageable pageable) {
        return payslipRepository.findMyPayslips(employeeId, pageable)
                .map(this::toPayslipResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollDtos.PayslipResponse getMyPayslipForPeriod(UUID employeeId, UUID periodId) {
        return toPayslipResponse(
                payslipRepository.findByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, employeeId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Payslip not found for this employee and period")));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private PayrollPeriod findPeriodOrThrow(UUID id) {
        return periodRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollPeriod", id));
    }

    private Payslip findPayslipOrThrow(UUID id) {
        return payslipRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", id));
    }

    private UUID getCurrentUserId() {
        // Returns null gracefully if no authenticated user (shouldn't happen in practice)
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof kz.aitu.hrms.modules.auth.entity.User user) {
                return user.getId();
            }
        } catch (Exception e) {
            log.warn("Could not resolve current user ID: {}", e.getMessage());
        }
        return null;
    }

    private PayrollDtos.PeriodResponse toPeriodResponse(PayrollPeriod p) {
        PayrollDtos.PeriodResponse r = new PayrollDtos.PeriodResponse();
        r.setId(p.getId());
        r.setYear(p.getYear());
        r.setMonth(p.getMonth());
        r.setName(p.getName());
        r.setStartDate(p.getStartDate());
        r.setEndDate(p.getEndDate());
        r.setWorkingDays(p.getWorkingDays());
        r.setStatus(p.getStatus());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());

        // Attach summary if payslips have been generated
        try {
            List<Object[]> totalsList = payslipRepository.getPeriodTotals(p.getId());
            if (totalsList != null && !totalsList.isEmpty()) {
                Object[] row = totalsList.get(0);
                PayrollDtos.PeriodSummary summary = new PayrollDtos.PeriodSummary();
                summary.setTotalGrossSalary(toBD(row[0]));
                summary.setTotalNetSalary(toBD(row[1]));
                summary.setTotalIpn(toBD(row[2]));
                summary.setTotalOpv(toBD(row[3]));
                summary.setTotalSo(toBD(row[4]));
                summary.setPayslipCount(toLong(row[5]));
                summary.setApprovedCount(
                        payslipRepository.countByPeriodIdAndStatusAndDeletedFalse(p.getId(), PayslipStatus.APPROVED));
                r.setSummary(summary);
            }
        } catch (Exception e) {
            log.debug("Could not load period summary for {}: {}", p.getId(), e.getMessage());
        }
        return r;
    }

    private PayrollDtos.PayslipResponse toPayslipResponse(Payslip s) {
        PayrollDtos.PayslipResponse r = new PayrollDtos.PayslipResponse();
        r.setId(s.getId());
        r.setWorkedDays(s.getWorkedDays());
        r.setTotalWorkingDays(s.getTotalWorkingDays());
        r.setGrossSalary(s.getGrossSalary());
        r.setEarnedSalary(s.getEarnedSalary());
        r.setAllowances(s.getAllowances());
        r.setOpvAmount(s.getOpvAmount());
        r.setOopvAmount(s.getOopvAmount());
        r.setTaxableIncome(s.getTaxableIncome());
        r.setIpnAmount(s.getIpnAmount());
        r.setOtherDeductions(s.getOtherDeductions());
        r.setTotalDeductions(s.getTotalDeductions());
        r.setNetSalary(s.getNetSalary());
        r.setSoAmount(s.getSoAmount());
        r.setSnAmount(s.getSnAmount());
        r.setMrpUsed(s.getMrpUsed());
        r.setResident(s.isResident());
        r.setStatus(s.getStatus());
        r.setPdfUrl(s.getPdfUrl());
        r.setCreatedAt(s.getCreatedAt());

        PayrollDtos.PeriodInfo pi = new PayrollDtos.PeriodInfo();
        pi.setId(s.getPeriod().getId());
        pi.setName(s.getPeriod().getName());
        pi.setYear(s.getPeriod().getYear());
        pi.setMonth(s.getPeriod().getMonth());
        r.setPeriod(pi);

        PayrollDtos.EmployeeInfo ei = new PayrollDtos.EmployeeInfo();
        Employee emp = s.getEmployee();
        ei.setId(emp.getId());
        ei.setEmployeeNumber(emp.getEmployeeNumber());
        ei.setFullName(emp.getFullName());
        ei.setEmail(emp.getEmail());
        if (emp.getDepartment() != null) ei.setDepartment(emp.getDepartment().getName());
        if (emp.getPosition() != null) ei.setPosition(emp.getPosition().getTitle());
        r.setEmployee(ei);

        return r;
    }

    private BigDecimal toBD(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        return new BigDecimal(o.toString());
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long l) return l;
        return Long.parseLong(o.toString());
    }
}
