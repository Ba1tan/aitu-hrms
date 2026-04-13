package kz.aitu.hrms.modules.dashboard.service;

import kz.aitu.hrms.modules.auth.entity.Role;
import kz.aitu.hrms.modules.auth.entity.User;
import kz.aitu.hrms.modules.dashboard.dto.DashboardDtos;
import kz.aitu.hrms.modules.employee.entity.EmploymentStatus;
import kz.aitu.hrms.modules.employee.repository.EmployeeRepository;
import kz.aitu.hrms.modules.payroll.enums.PayrollPeriodStatus;
import kz.aitu.hrms.modules.payroll.repository.PayrollPeriodRepository;
import kz.aitu.hrms.modules.payroll.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final PayrollPeriodRepository periodRepository;
    private final PayslipRepository payslipRepository;
    // leave and attendance repositories will be injected here
    // i am waiting when those modules are will be implemented by Askar:
    // private final LeaveRequestRepository leaveRequestRepository;
    // private final AttendanceRecordRepository attendanceRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardDtos.DashboardStatsResponse getStats(User currentUser) {
        Role role = currentUser.getRole();
        DashboardDtos.DashboardStatsResponse.DashboardStatsResponseBuilder builder =
                DashboardDtos.DashboardStatsResponse.builder();
        if (role != Role.EMPLOYEE) {
            buildEmployeeStats(builder);
        }
        if (role == Role.SUPER_ADMIN || role == Role.HR_MANAGER || role == Role.ACCOUNTANT) {
            buildPayrollStats(builder);
        }
        if (role == Role.SUPER_ADMIN || role == Role.HR_MANAGER || role == Role.MANAGER) {
            buildLeaveStats(builder);
        }
        if (role == Role.SUPER_ADMIN || role == Role.HR_MANAGER || role == Role.MANAGER) {
            buildAttendanceStats(builder);
        }
        if (currentUser.getEmployeeId() != null) {
            buildPersonalStats(builder, currentUser);
        }
        return builder.build();
    }

    private void buildEmployeeStats(
            DashboardDtos.DashboardStatsResponse.DashboardStatsResponseBuilder builder) {
        try {
            long total  = employeeRepository.countByDeletedFalse();
            long active = employeeRepository.countByStatusAndDeletedFalse(EmploymentStatus.ACTIVE);
            long onLeave = employeeRepository.countByStatusAndDeletedFalse(EmploymentStatus.ON_LEAVE);

            LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
            long newHires = employeeRepository.countByHireDateGreaterThanEqualAndDeletedFalse(firstOfMonth);

            builder.totalEmployees((int) total)
                   .activeEmployees((int) active)
                   .onLeaveEmployees((int) onLeave)
                   .newHiresThisMonth((int) newHires);
        } catch (Exception e) {
            log.warn("Could not build employee stats: {}", e.getMessage());
        }
    }

    private void buildPayrollStats(
            DashboardDtos.DashboardStatsResponse.DashboardStatsResponseBuilder builder) {
        try {
            periodRepository
                .findTopByStatusNotAndDeletedFalseOrderByYearDescMonthDesc(PayrollPeriodStatus.LOCKED)
                .ifPresent(period -> {
                    builder.lastPayrollPeriodName(period.getName())
                           .lastPayrollStatus(period.getStatus().name());

                    List<Object[]> totals = payslipRepository.getPeriodTotals(period.getId());
                    if (!totals.isEmpty()) {
                        Object[] row = totals.get(0);
                        builder.lastPayrollGross(toBD(row[0]))
                               .lastPayrollNet(toBD(row[1]))
                               .lastPayrollEmployeeCount(((Number) row[5]).intValue());
                    }
                });
        } catch (Exception e) {
            log.warn("Could not build payroll stats: {}", e.getMessage());
        }
    }

    private void buildLeaveStats(
            DashboardDtos.DashboardStatsResponse.DashboardStatsResponseBuilder builder) {
        // TODO: implement when Askar adds leave module
        // long pending = leaveRequestRepository.countByStatusAndDeletedFalse(LeaveStatus.PENDING);
        // builder.pendingLeaveRequests((int) pending);
        builder.pendingLeaveRequests(0);  // placeholder until leave module is done
    }

    private void buildAttendanceStats(
            DashboardDtos.DashboardStatsResponse.DashboardStatsResponseBuilder builder) {
        // TODO: implement when Askar adds attendance module
        // LocalDate today = LocalDate.now();
        // long present = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.PRESENT);

        builder.todayPresent(0).todayAbsent(0).todayLate(0)
               .todayTotal(0).attendanceRate(0.0);  // placeholder
    }

    private void buildPersonalStats(
            DashboardDtos.DashboardStatsResponse.DashboardStatsResponseBuilder builder,
            User currentUser) {
        try {
            payslipRepository
                .findMyPayslips(currentUser.getEmployeeId(), PageRequest.of(0, 1))
                .getContent()
                .stream().findFirst()
                .ifPresent(payslip -> {
                    builder.myLastNetSalary(payslip.getNetSalary())
                           .myLastPayrollPeriod(payslip.getPeriod().getName());
                });

            // TODO: own leave balance — implement when Askar adds leave module
            // leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, currentYear)
            //   .stream().filter(b -> b.getLeaveType().getName().equals("Annual Leave"))
            //   .findFirst().ifPresent(b -> builder.myLeaveBalance(b.getRemainingDays()));
            builder.myLeaveBalance(0);  // placeholder

            // TODO: own attendance today — implement when attendance module is done
            builder.myAttendanceTodayStatus(null);  // null = not checked in yet

        } catch (Exception e) {
            log.warn("Could not build personal stats for user {}: {}", currentUser.getEmail(), e.getMessage());
        }
    }

    private BigDecimal toBD(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }
}
