package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.payroll.calculator.EmployeePayrollProfile;
import kz.aitu.hrms.payroll.dto.PayslipDtos;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import kz.aitu.hrms.payroll.repository.PayslipRepository;
import kz.aitu.hrms.payroll.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipService {

    private final PayslipRepository payslipRepo;
    private final PayslipFactory payslipFactory;
    private final EmployeeLookup employeeLookup;
    private final PayrollMapper mapper;
    private final EventPublisher events;

    @Transactional(readOnly = true)
    public Page<PayslipDtos.Response> findByPeriod(UUID periodId,
                                                   PayslipStatus status,
                                                   String search,
                                                   Pageable pageable) {
        return payslipRepo.search(periodId, status, blankToNull(search), pageable)
                .map(mapper::toPayslipResponse);
    }

    @Transactional(readOnly = true)
    public PayslipDtos.Response detail(UUID payslipId) {
        return mapper.toPayslipResponse(requirePayslip(payslipId));
    }

    @Transactional
    public PayslipDtos.Response adjust(UUID payslipId, PayslipDtos.AdjustRequest req) {
        Payslip payslip = requirePayslip(payslipId);
        if (payslip.getStatus() != PayslipStatus.DRAFT) {
            throw new BusinessException(
                    "Only DRAFT payslips can be adjusted (current: " + payslip.getStatus() + ")");
        }
        if (payslip.getPeriod().getStatus() == PayrollPeriodStatus.LOCKED) {
            throw new BusinessException("Cannot adjust payslip in a locked period");
        }

        EmployeePayrollProfile profile = employeeLookup.profileOrThrow(payslip.getEmployeeId());
        PayslipDtos.Response before = mapper.toPayslipResponse(payslip);
        // Use existing on-payslip values when the request omits a field.
        int workedDays = req.getWorkedDays() != null ? req.getWorkedDays() : payslip.getWorkedDays();
        BigDecimal allowances = req.getAllowances() != null ? req.getAllowances() : payslip.getAllowances();
        BigDecimal otherDed = req.getOtherDeductions() != null ? req.getOtherDeductions() : payslip.getOtherDeductions();

        payslipFactory.recalculate(payslip, profile, workedDays, allowances, otherDed);
        PayslipDtos.Response after = mapper.toPayslipResponse(payslipRepo.save(payslip));
        events.audit("ADJUST", "PAYSLIP", payslipId, before, after);
        return after;
    }

    @Transactional
    public PayslipDtos.Response recalculate(UUID payslipId) {
        Payslip payslip = requirePayslip(payslipId);
        if (payslip.getPeriod().getStatus() == PayrollPeriodStatus.LOCKED) {
            throw new BusinessException("Cannot recalculate payslip in a locked period");
        }
        EmployeePayrollProfile profile = employeeLookup.profileOrThrow(payslip.getEmployeeId());
        PayslipDtos.Response before = mapper.toPayslipResponse(payslip);
        payslipFactory.recalculate(
                payslip, profile,
                payslip.getWorkedDays(),
                payslip.getAllowances(),
                payslip.getOtherDeductions());
        PayslipDtos.Response after = mapper.toPayslipResponse(payslipRepo.save(payslip));
        events.audit("RECALCULATE", "PAYSLIP", payslipId, before, after);
        return after;
    }

    // ── Self-service ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PayslipDtos.Response> myPayslips(Pageable pageable) {
        UUID employeeId = requireEmployeeId();
        return payslipRepo.findMyPayslips(employeeId, pageable).map(mapper::toPayslipResponse);
    }

    @Transactional(readOnly = true)
    public PayslipDtos.Response myPayslipForPeriod(UUID periodId) {
        UUID employeeId = requireEmployeeId();
        Payslip p = payslipRepo.findByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payslip not found for this employee and period"));
        return mapper.toPayslipResponse(p);
    }

    /**
     * Verify the caller has permission to view a specific payslip.
     * HR/accountants (PAYROLL_VIEW) can view any; everyone else only their own.
     */
    public Payslip requireViewablePayslip(UUID payslipId) {
        Payslip p = requirePayslip(payslipId);
        if (CurrentUser.hasAuthority("PAYROLL_VIEW")) return p;
        UUID self = CurrentUser.employeeId();
        if (self != null && self.equals(p.getEmployeeId())) return p;
        throw new AccessDeniedException("Not allowed to view this payslip");
    }

    Payslip requirePayslip(UUID payslipId) {
        return payslipRepo.findByIdAndDeletedFalse(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", payslipId));
    }

    private UUID requireEmployeeId() {
        UUID id = CurrentUser.employeeId();
        if (id == null) {
            throw new BusinessException(
                    "Your account is not linked to an employee profile. Contact HR.");
        }
        return id;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}