package kz.aitu.hrms.payroll.service;

import kz.aitu.hrms.payroll.calculator.EmployeePayrollProfile;
import kz.aitu.hrms.payroll.calculator.KazakhstanPayrollCalculator;
import kz.aitu.hrms.payroll.calculator.PayrollCalculationResult;
import kz.aitu.hrms.payroll.client.AttendanceClient;
import kz.aitu.hrms.payroll.entity.AdditionType;
import kz.aitu.hrms.payroll.entity.PayrollAddition;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import kz.aitu.hrms.payroll.repository.PayrollAdditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Builds a {@link Payslip} for one (employee, period) tuple by:
 *   1. fetching worked days from attendance-service (falls back to full month)
 *   2. summing additions for the period (allowances vs. deductions)
 *   3. running {@link KazakhstanPayrollCalculator}
 *   4. composing the {@link Payslip} with an employee snapshot
 *
 * Does not persist or call the AI service — that is the orchestrator's job.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayslipFactory {

    private final KazakhstanPayrollCalculator calculator;
    private final AttendanceClient attendanceClient;
    private final PayrollAdditionRepository additionRepo;

    @Value("${app.payroll.anomaly-flag-threshold}")
    private BigDecimal anomalyFlagThreshold;

    public Payslip build(EmployeePayrollProfile profile, PayrollPeriod period) {
        int workedDays = resolveWorkedDays(profile.getEmployeeId(), period);
        AdditionTotals add = sumAdditions(profile.getEmployeeId(), period.getId());

        PayrollCalculationResult r = calculator.calculate(
                profile,
                workedDays,
                period.getWorkingDays(),
                add.allowances(),
                add.deductions(),
                String.valueOf(period.getYear()),
                period.getName()
        );

        return Payslip.builder()
                .period(period)
                .employeeId(profile.getEmployeeId())
                .employeeIin(profile.getIin())
                .employeeName(profile.getFullName())
                .employeeNumber(profile.getEmployeeNumber())
                .departmentName(profile.getDepartmentName())
                .positionTitle(profile.getPositionTitle())
                .workedDays(r.getWorkedDays())
                .totalWorkingDays(r.getTotalWorkingDays())
                .grossSalary(r.getGrossSalary())
                .earnedSalary(r.getEarnedSalary())
                .allowances(r.getAllowances())
                .otherDeductions(r.getDeductions())
                .opvAmount(r.getOpvAmount())
                .vosmsAmount(r.getVosmsAmount())
                .oopvAmount(r.getOopvAmount())
                .taxableIncome(r.getTaxableIncome())
                .ipnAmount(r.getIpnAmount())
                .totalDeductions(r.getTotalDeductions())
                .netSalary(r.getNetSalary())
                .soAmount(r.getSoAmount())
                .snAmount(r.getSnAmount())
                .opvrAmount(r.getOpvrAmount())
                .mrpUsed(r.getMrpUsed())
                .resident(r.isResident())
                .disability(r.isDisability())
                .status(PayslipStatus.DRAFT)
                .build();
    }

    /**
     * Recompute the payslip's tax fields after an adjustment. Mutates and
     * returns the same instance for caller convenience.
     */
    public Payslip recalculate(Payslip payslip,
                               EmployeePayrollProfile profile,
                               int workedDays,
                               BigDecimal allowances,
                               BigDecimal otherDeductions) {
        PayrollCalculationResult r = calculator.calculate(
                profile,
                workedDays,
                payslip.getTotalWorkingDays(),
                allowances,
                otherDeductions,
                String.valueOf(payslip.getPeriod().getYear()),
                payslip.getPeriod().getName()
        );
        payslip.setWorkedDays(r.getWorkedDays());
        payslip.setEarnedSalary(r.getEarnedSalary());
        payslip.setAllowances(r.getAllowances());
        payslip.setOtherDeductions(r.getDeductions());
        payslip.setOpvAmount(r.getOpvAmount());
        payslip.setVosmsAmount(r.getVosmsAmount());
        payslip.setOopvAmount(r.getOopvAmount());
        payslip.setTaxableIncome(r.getTaxableIncome());
        payslip.setIpnAmount(r.getIpnAmount());
        payslip.setTotalDeductions(r.getTotalDeductions());
        payslip.setNetSalary(r.getNetSalary());
        payslip.setSoAmount(r.getSoAmount());
        payslip.setSnAmount(r.getSnAmount());
        payslip.setOpvrAmount(r.getOpvrAmount());
        payslip.setMrpUsed(r.getMrpUsed());
        return payslip;
    }

    public BigDecimal getAnomalyFlagThreshold() {
        return anomalyFlagThreshold;
    }

    private int resolveWorkedDays(UUID employeeId, PayrollPeriod period) {
        try {
            var resp = attendanceClient.summary(employeeId, period.getYear(), period.getMonth());
            if (resp != null && resp.data() != null) {
                AttendanceClient.EmployeeMonthSummary s = resp.data();
                long worked = s.presentDays() + s.lateDays() + s.halfDays();
                if (worked > 0) {
                    return (int) Math.min(worked, period.getWorkingDays());
                }
            }
        } catch (Exception e) {
            log.debug("attendance summary unavailable for {} {}-{}: {}",
                    employeeId, period.getYear(), period.getMonth(), e.getMessage());
        }
        return period.getWorkingDays();
    }

    private AdditionTotals sumAdditions(UUID employeeId, UUID periodId) {
        List<PayrollAddition> additions =
                additionRepo.findByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, employeeId);
        BigDecimal allow = BigDecimal.ZERO;
        BigDecimal ded = BigDecimal.ZERO;
        for (PayrollAddition a : additions) {
            if (a.getType() == AdditionType.BONUS) {
                allow = allow.add(a.getAmount());
            } else {
                ded = ded.add(a.getAmount());
            }
        }
        return new AdditionTotals(allow, ded);
    }

    private record AdditionTotals(BigDecimal allowances, BigDecimal deductions) {}
}