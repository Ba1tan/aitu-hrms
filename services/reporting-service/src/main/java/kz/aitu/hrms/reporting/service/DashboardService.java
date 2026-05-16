package kz.aitu.hrms.reporting.service;

import kz.aitu.hrms.reporting.client.*;
import kz.aitu.hrms.reporting.client.dto.*;
import kz.aitu.hrms.reporting.dto.dashboard.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private static final Set<String> CAN_SEE_EMPLOYEES =
            Set.of("SUPER_ADMIN", "HR_MANAGER", "ACCOUNTANT", "MANAGER", "TEAM_LEAD", "DIRECTOR", "HR_SPECIALIST");
    private static final Set<String> CAN_SEE_PAYROLL =
            Set.of("SUPER_ADMIN", "HR_MANAGER", "ACCOUNTANT", "DIRECTOR");
    private static final Set<String> CAN_SEE_LEAVE =
            Set.of("SUPER_ADMIN", "HR_MANAGER", "MANAGER", "DIRECTOR");
    private static final Set<String> CAN_SEE_ATTENDANCE =
            Set.of("SUPER_ADMIN", "HR_MANAGER", "MANAGER", "DIRECTOR");

    private final EmployeeClient employeeClient;
    private final PayrollClient payrollClient;
    private final AttendanceClient attendanceClient;
    private final LeaveClient leaveClient;

    @Cacheable(value = "dashboard", key = "#userId + ':' + #role")
    public DashboardStatsDto build(UUID userId, String role, UUID employeeId) {
        DashboardStatsDto.DashboardStatsDtoBuilder b = DashboardStatsDto.builder();

        if (CAN_SEE_EMPLOYEES.contains(role)) {
            b.employees(safeCall(this::buildEmployeesSection, "employees"));
        }
        if (CAN_SEE_PAYROLL.contains(role)) {
            b.lastPayroll(safeCall(this::buildPayrollSection, "lastPayroll"));
        }
        if (CAN_SEE_LEAVE.contains(role)) {
            b.leave(safeCall(this::buildLeaveSection, "leave"));
        }
        if (CAN_SEE_ATTENDANCE.contains(role)) {
            b.attendanceToday(safeCall(this::buildAttendanceSection, "attendanceToday"));
        }
        if (employeeId != null) {
            b.personal(safeCall(() -> buildPersonalSection(employeeId), "personal"));
        }
        return b.build();
    }

    private EmployeesSectionDto buildEmployeesSection() {
        EmployeeCountsDto counts = getEmployeeCounts();
        return EmployeesSectionDto.builder()
                .totalEmployees(counts.getTotal())
                .activeEmployees(counts.getActive())
                .onLeaveEmployees(counts.getOnLeave())
                .newHiresThisMonth(counts.getNewHiresThisMonth())
                .build();
    }

    private EmployeeCountsDto getEmployeeCounts() {
        try {
            return employeeClient.getCounts();
        } catch (Exception e) {
            log.debug("Employee counts fast path unavailable, falling back to pagination: {}", e.getMessage());
            return countByPagination();
        }
    }

    private EmployeeCountsDto countByPagination() {
        PageResponse<EmployeeSummaryDto> page = employeeClient.list(null, null, 0, 1);
        EmployeeCountsDto dto = new EmployeeCountsDto();
        dto.setTotal(page.getTotalElements());
        PageResponse<EmployeeSummaryDto> active = employeeClient.list(null, "ACTIVE", 0, 1);
        dto.setActive(active.getTotalElements());
        PageResponse<EmployeeSummaryDto> onLeave = employeeClient.list(null, "ON_LEAVE", 0, 1);
        dto.setOnLeave(onLeave.getTotalElements());
        return dto;
    }

    private PayrollSectionDto buildPayrollSection() {
        PayrollPeriodDto period = getLatestPeriod();
        if (period == null) return null;

        PayrollTotalsDto totals = getPeriodTotals(period.getId());

        return PayrollSectionDto.builder()
                .lastPayrollPeriodName(period.getName())
                .lastPayrollStatus(period.getStatus())
                .lastPayrollGross(totals != null ? totals.getTotalGross() : null)
                .lastPayrollNet(totals != null ? totals.getTotalNet() : null)
                .lastPayrollEmployeeCount(totals != null ? totals.getEmployeeCount() : 0)
                .build();
    }

    private PayrollPeriodDto getLatestPeriod() {
        try {
            return payrollClient.getLatestPeriod();
        } catch (Exception e) {
            log.debug("Latest period fast path unavailable, falling back to list: {}", e.getMessage());
            PageResponse<PayrollPeriodDto> page = payrollClient.listPeriods(0, 5);
            if (page == null || page.getContent() == null || page.getContent().isEmpty()) return null;
            return page.getContent().stream()
                    .filter(p -> !p.isLocked())
                    .findFirst()
                    .orElse(page.getContent().get(0));
        }
    }

    private PayrollTotalsDto getPeriodTotals(UUID periodId) {
        try {
            return payrollClient.getPeriodTotals(periodId);
        } catch (Exception e) {
            log.debug("Period totals fast path unavailable, aggregating from payslips: {}", e.getMessage());
            return aggregateTotals(periodId);
        }
    }

    private PayrollTotalsDto aggregateTotals(UUID periodId) {
        PageResponse<PayslipDto> page = payrollClient.listPayslips(periodId, 0, 500);
        if (page == null || page.getContent() == null) return null;
        List<PayslipDto> slips = page.getContent();
        PayrollTotalsDto dto = new PayrollTotalsDto();
        dto.setTotalGross(slips.stream().map(PayslipDto::getGrossSalary)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalNet(slips.stream().map(PayslipDto::getNetSalary)
                .filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setEmployeeCount(slips.size());
        return dto;
    }

    private LeaveSectionDto buildLeaveSection() {
        long count = getPendingLeaveCount();
        return LeaveSectionDto.builder().pendingLeaveRequests(count).build();
    }

    private long getPendingLeaveCount() {
        try {
            return leaveClient.pendingCount().getCount();
        } catch (Exception e) {
            log.debug("Leave pending count fast path unavailable, using page totalElements: {}", e.getMessage());
            PageResponse<Object> page = leaveClient.pendingRequests(0, 1);
            return page != null ? page.getTotalElements() : 0;
        }
    }

    private AttendanceSectionDto buildAttendanceSection() {
        AttendanceTodayCountsDto counts = attendanceClient.getTodayCounts(null);
        long total = counts.getTotal() > 0 ? counts.getTotal()
                : counts.getPresent() + counts.getAbsent() + counts.getLate();
        BigDecimal rate = total > 0
                ? BigDecimal.valueOf(counts.getPresent()).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        return AttendanceSectionDto.builder()
                .todayPresent(counts.getPresent())
                .todayAbsent(counts.getAbsent())
                .todayLate(counts.getLate())
                .todayTotal(total)
                .attendanceRate(rate)
                .build();
    }

    private PersonalSectionDto buildPersonalSection(UUID employeeId) {
        BigDecimal netSalary = null;
        String periodName = null;
        try {
            List<PayslipDto> slips = payrollClient.myPayslips(1);
            if (slips != null && !slips.isEmpty()) {
                netSalary = slips.get(0).getNetSalary();
                periodName = slips.get(0).getPeriodName();
            }
        } catch (Exception e) {
            log.debug("Personal payslip unavailable: {}", e.getMessage());
        }

        BigDecimal leaveBalance = null;
        try {
            List<LeaveBalanceDto> balances = leaveClient.myBalances();
            if (balances != null && !balances.isEmpty()) {
                leaveBalance = balances.stream()
                        .map(LeaveBalanceDto::getRemainingDays)
                        .filter(d -> d != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
        } catch (Exception e) {
            log.debug("Personal leave balance unavailable: {}", e.getMessage());
        }

        String todayStatus = null;
        try {
            AttendanceRecordDto record = attendanceClient.myToday();
            if (record != null) todayStatus = record.getStatus();
        } catch (Exception e) {
            log.debug("Personal attendance unavailable: {}", e.getMessage());
        }

        return PersonalSectionDto.builder()
                .myLastNetSalary(netSalary)
                .myLastPayrollPeriod(periodName)
                .myLeaveBalance(leaveBalance)
                .myAttendanceTodayStatus(todayStatus)
                .build();
    }

    private <T> T safeCall(Supplier<T> block, String section) {
        try {
            return block.get();
        } catch (Exception e) {
            log.warn("Dashboard section [{}] failed: {}", section, e.getMessage());
            return null;
        }
    }
}
