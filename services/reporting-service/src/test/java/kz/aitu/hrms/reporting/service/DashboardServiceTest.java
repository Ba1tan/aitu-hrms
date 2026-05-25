package kz.aitu.hrms.reporting.service;

import kz.aitu.hrms.reporting.client.*;
import kz.aitu.hrms.reporting.client.dto.*;
import kz.aitu.hrms.reporting.dto.dashboard.DashboardStatsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private EmployeeClient employeeClient;
    @Mock private PayrollClient payrollClient;
    @Mock private AttendanceClient attendanceClient;
    @Mock private LeaveClient leaveClient;

    @InjectMocks private DashboardService service;

    private UUID userId = UUID.randomUUID();
    private UUID empId = UUID.randomUUID();

    @Test
    void superAdmin_getsAllSections() {
        stubEmployees();
        stubPayroll();
        stubLeave();
        stubAttendance();
        stubPersonal();

        DashboardStatsDto dto = service.build(userId, "SUPER_ADMIN", empId);

        assertThat(dto.getEmployees()).isNotNull();
        assertThat(dto.getLastPayroll()).isNotNull();
        assertThat(dto.getLeave()).isNotNull();
        assertThat(dto.getAttendanceToday()).isNotNull();
        assertThat(dto.getPersonal()).isNotNull();
    }

    @Test
    void employee_getsOnlyPersonalSection() {
        stubPersonal();

        DashboardStatsDto dto = service.build(userId, "EMPLOYEE", empId);

        assertThat(dto.getEmployees()).isNull();
        assertThat(dto.getLastPayroll()).isNull();
        assertThat(dto.getLeave()).isNull();
        assertThat(dto.getAttendanceToday()).isNull();
        assertThat(dto.getPersonal()).isNotNull();
    }

    @Test
    void nullEmployeeId_noPersonalSection() {
        stubEmployees();

        DashboardStatsDto dto = service.build(userId, "TEAM_LEAD", null);

        assertThat(dto.getPersonal()).isNull();
        assertThat(dto.getEmployees()).isNotNull();
    }

    @Test
    void employeeClientFails_employeesSectionIsNull_otherSectionsOk() {
        when(employeeClient.getCounts()).thenThrow(new RuntimeException("down"));
        stubPagedEmployeesFail();
        stubPayroll();

        DashboardStatsDto dto = service.build(userId, "ACCOUNTANT", null);

        assertThat(dto.getEmployees()).isNull();
        assertThat(dto.getLastPayroll()).isNotNull();
    }

    @Test
    void accountant_getsEmployeesAndPayroll() {
        stubEmployees();
        stubPayroll();

        DashboardStatsDto dto = service.build(userId, "ACCOUNTANT", null);

        assertThat(dto.getEmployees()).isNotNull();
        assertThat(dto.getLastPayroll()).isNotNull();
        assertThat(dto.getLeave()).isNull();
        assertThat(dto.getAttendanceToday()).isNull();
    }

    @Test
    void manager_getsEmployeesLeaveAttendance() {
        stubEmployees();
        stubLeave();
        stubAttendance();
        stubPersonal();

        DashboardStatsDto dto = service.build(userId, "MANAGER", empId);

        assertThat(dto.getEmployees()).isNotNull();
        assertThat(dto.getLeave()).isNotNull();
        assertThat(dto.getAttendanceToday()).isNotNull();
        assertThat(dto.getLastPayroll()).isNull();
    }

    @Test
    void attendanceRateCalculatedCorrectly() {
        stubEmployees();
        stubPayroll();
        stubLeave();
        stubPersonal();

        AttendanceTodayCountsDto counts = new AttendanceTodayCountsDto();
        counts.setPresent(80);
        counts.setAbsent(10);
        counts.setLate(10);
        counts.setTotal(100);
        when(attendanceClient.getTodayCounts(null)).thenReturn(counts);

        DashboardStatsDto dto = service.build(UUID.randomUUID(), "SUPER_ADMIN", empId);

        assertThat(dto.getAttendanceToday().getAttendanceRate())
                .isEqualByComparingTo(new BigDecimal("0.8000"));
    }

    // --- stubs ---

    private void stubEmployees() {
        EmployeeCountsDto counts = new EmployeeCountsDto();
        counts.setTotal(100);
        counts.setActive(90);
        counts.setOnLeave(10);
        counts.setNewHiresThisMonth(5);
        when(employeeClient.getCounts()).thenReturn(counts);
    }

    private void stubPagedEmployeesFail() {
        when(employeeClient.list(any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("down"));
    }

    private void stubPayroll() {
        PayrollPeriodDto period = new PayrollPeriodDto();
        period.setId(UUID.randomUUID());
        period.setName("Май 2026");
        period.setStatus("CLOSED");
        when(payrollClient.getLatestPeriod()).thenReturn(period);

        PayrollTotalsDto totals = new PayrollTotalsDto();
        totals.setTotalGross(BigDecimal.valueOf(5_000_000));
        totals.setTotalNet(BigDecimal.valueOf(4_200_000));
        totals.setEmployeeCount(90);
        when(payrollClient.getPeriodTotals(period.getId())).thenReturn(totals);
    }

    private void stubLeave() {
        LeavePendingCountDto dto = new LeavePendingCountDto();
        dto.setCount(3);
        when(leaveClient.pendingCount()).thenReturn(dto);
    }

    private void stubAttendance() {
        AttendanceTodayCountsDto counts = new AttendanceTodayCountsDto();
        counts.setPresent(85);
        counts.setAbsent(5);
        counts.setLate(10);
        counts.setTotal(100);
        when(attendanceClient.getTodayCounts(null)).thenReturn(counts);
    }

    private void stubPersonal() {
        PayslipDto.PeriodInfo per = new PayslipDto.PeriodInfo();
        per.setName("Май 2026");
        PayslipDto slip = new PayslipDto();
        slip.setNetSalary(BigDecimal.valueOf(350000));
        slip.setPeriod(per);
        when(payrollClient.myPayslips(1)).thenReturn(List.of(slip));

        LeaveBalanceDto bal = new LeaveBalanceDto();
        bal.setRemainingDays(BigDecimal.valueOf(14));
        when(leaveClient.myBalances()).thenReturn(List.of(bal));

        AttendanceRecordDto rec = new AttendanceRecordDto();
        rec.setStatus("PRESENT");
        when(attendanceClient.myToday()).thenReturn(rec);
    }
}
