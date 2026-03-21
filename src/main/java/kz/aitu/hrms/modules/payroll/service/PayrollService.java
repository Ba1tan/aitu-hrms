package kz.aitu.hrms.modules.payroll.service;

import kz.aitu.hrms.modules.payroll.dto.PayrollDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PayrollService {

    PayrollDtos.PeriodResponse createPeriod(PayrollDtos.CreatePeriodRequest request);

    Page<PayrollDtos.PeriodResponse> getPeriods(Pageable pageable);

    PayrollDtos.PeriodResponse getPeriod(UUID periodId);

    PayrollDtos.GeneratePayslipsResponse generatePayslips(
            UUID periodId, PayrollDtos.GeneratePayslipsRequest request);

    Page<PayrollDtos.PayslipResponse> getPayslipsByPeriod(UUID periodId, Pageable pageable);

    PayrollDtos.PayslipResponse getPayslip(UUID payslipId);

    PayrollDtos.PayslipResponse adjustPayslip(UUID payslipId, PayrollDtos.AdjustPayslipRequest request);

    PayrollDtos.PeriodResponse approvePeriod(UUID periodId);   // APPROVED → PAID is next step
    PayrollDtos.PeriodResponse markPeriodPaid(UUID periodId);
    PayrollDtos.PeriodResponse lockPeriod(UUID periodId);

    Page<PayrollDtos.PayslipResponse> getMyPayslips(UUID employeeId, Pageable pageable);

    PayrollDtos.PayslipResponse getMyPayslipForPeriod(UUID employeeId, UUID periodId);
}
