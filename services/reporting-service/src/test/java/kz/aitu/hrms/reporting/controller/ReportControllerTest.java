package kz.aitu.hrms.reporting.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.common.security.JwtAuthenticationFilter;
import kz.aitu.hrms.reporting.service.pdf.reports.*;
import kz.aitu.hrms.reporting.service.xlsx.reports.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReportController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
                org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration.class
        })
@Import(ReportControllerTest.MethodSecurityConfig.class)
class ReportControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {}

    @Autowired MockMvc mvc;

    @MockBean PayrollSummaryXlsx payrollSummaryXlsx;
    @MockBean PayrollSummaryPdf payrollSummaryPdf;
    @MockBean Form200Xlsx form200Xlsx;
    @MockBean SalaryBreakdownXlsx salaryBreakdownXlsx;
    @MockBean AttendanceMonthlyXlsx attendanceMonthlyXlsx;
    @MockBean AttendanceSummaryXlsx attendanceSummaryXlsx;
    @MockBean LeaveBalancesXlsx leaveBalancesXlsx;
    @MockBean EmployeeDirectoryXlsx employeeDirectoryXlsx;
    @MockBean TurnoverXlsx turnoverXlsx;
    @MockBean HeadcountXlsx headcountXlsx;
    @MockBean ExecutiveSummaryPdf executiveSummaryPdf;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF  = "application/pdf";

    @BeforeEach
    void configureFilter() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    // ── payroll-summary ──────────────────────────────────────────────────────

    @Test
    void payrollSummary_withPermission_returnsXlsx() throws Exception {
        doNothing().when(payrollSummaryXlsx).write(any(), any());
        mvc.perform(get("/v1/reports/payroll-summary")
                        .param("periodId", UUID.randomUUID().toString())
                        .with(authentication(auth("REPORT_PAYROLL"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    @Test
    void payrollSummary_withoutPermission_returns403() throws Exception {
        mvc.perform(get("/v1/reports/payroll-summary")
                        .param("periodId", UUID.randomUUID().toString())
                        .with(authentication(auth("REPORT_ATTENDANCE"))))
                .andExpect(status().isForbidden());
    }

    // ── payroll-summary/pdf ───────────────────────────────────────────────────

    @Test
    void payrollSummaryPdf_withPermission_returnsPdf() throws Exception {
        doNothing().when(payrollSummaryPdf).write(any(), any());
        mvc.perform(get("/v1/reports/payroll-summary/pdf")
                        .param("periodId", UUID.randomUUID().toString())
                        .with(authentication(auth("REPORT_PAYROLL"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", PDF));
    }

    // ── form200 ───────────────────────────────────────────────────────────────

    @Test
    void form200_withPermission_returnsXlsx() throws Exception {
        doNothing().when(form200Xlsx).write(anyInt(), anyInt(), any());
        mvc.perform(get("/v1/reports/form200")
                        .param("year", "2026").param("quarter", "2")
                        .with(authentication(auth("REPORT_PAYROLL"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    @Test
    void form200_invalidYear_returns400() throws Exception {
        mvc.perform(get("/v1/reports/form200")
                        .param("year", "1999").param("quarter", "1")
                        .with(authentication(auth("REPORT_PAYROLL"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void form200_invalidQuarter_returns400() throws Exception {
        mvc.perform(get("/v1/reports/form200")
                        .param("year", "2026").param("quarter", "5")
                        .with(authentication(auth("REPORT_PAYROLL"))))
                .andExpect(status().isBadRequest());
    }

    // ── salary-breakdown ──────────────────────────────────────────────────────

    @Test
    void salaryBreakdown_withPermission_returnsXlsx() throws Exception {
        doNothing().when(salaryBreakdownXlsx).write(any(), any());
        mvc.perform(get("/v1/reports/salary-breakdown")
                        .with(authentication(auth("REPORT_PAYROLL"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    // ── attendance-monthly ────────────────────────────────────────────────────

    @Test
    void attendanceMonthly_withPermission_returnsXlsx() throws Exception {
        doNothing().when(attendanceMonthlyXlsx).write(anyInt(), anyInt(), any());
        mvc.perform(get("/v1/reports/attendance-monthly")
                        .param("year", "2026").param("month", "5")
                        .with(authentication(auth("REPORT_ATTENDANCE"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    @Test
    void attendanceMonthly_invalidMonth_returns400() throws Exception {
        mvc.perform(get("/v1/reports/attendance-monthly")
                        .param("year", "2026").param("month", "13")
                        .with(authentication(auth("REPORT_ATTENDANCE"))))
                .andExpect(status().isBadRequest());
    }

    // ── attendance-summary ────────────────────────────────────────────────────

    @Test
    void attendanceSummary_withPermission_returnsXlsx() throws Exception {
        doNothing().when(attendanceSummaryXlsx).write(anyInt(), anyInt(), any());
        mvc.perform(get("/v1/reports/attendance-summary")
                        .param("year", "2026").param("month", "5")
                        .with(authentication(auth("REPORT_ATTENDANCE"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    // ── leave-balances ────────────────────────────────────────────────────────

    @Test
    void leaveBalances_withPermission_returnsXlsx() throws Exception {
        doNothing().when(leaveBalancesXlsx).write(anyInt(), any());
        mvc.perform(get("/v1/reports/leave-balances")
                        .param("year", "2026")
                        .with(authentication(auth("REPORT_LEAVE"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    @Test
    void leaveBalances_withoutPermission_returns403() throws Exception {
        mvc.perform(get("/v1/reports/leave-balances")
                        .param("year", "2026")
                        .with(authentication(auth("REPORT_PAYROLL"))))
                .andExpect(status().isForbidden());
    }

    // ── employee-directory ────────────────────────────────────────────────────

    @Test
    void employeeDirectory_withPermission_returnsXlsx() throws Exception {
        doNothing().when(employeeDirectoryXlsx).write(any());
        mvc.perform(get("/v1/reports/employee-directory")
                        .with(authentication(auth("REPORT_HR"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    @Test
    void employeeDirectory_withoutPermission_returns403() throws Exception {
        mvc.perform(get("/v1/reports/employee-directory")
                        .with(authentication(auth("SOME_OTHER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeDirectory_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/v1/reports/employee-directory"))
                .andExpect(status().isUnauthorized());
    }

    // ── turnover ──────────────────────────────────────────────────────────────

    @Test
    void turnover_withPermission_returnsXlsx() throws Exception {
        doNothing().when(turnoverXlsx).write(anyInt(), any());
        mvc.perform(get("/v1/reports/turnover")
                        .param("year", "2026")
                        .with(authentication(auth("REPORT_HR"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    @Test
    void turnover_withoutPermission_returns403() throws Exception {
        mvc.perform(get("/v1/reports/turnover")
                        .param("year", "2026")
                        .with(authentication(auth("REPORT_PAYROLL"))))
                .andExpect(status().isForbidden());
    }

    // ── headcount ─────────────────────────────────────────────────────────────

    @Test
    void headcount_withPermission_returnsXlsx() throws Exception {
        doNothing().when(headcountXlsx).write(any(), any(), any());
        mvc.perform(get("/v1/reports/headcount")
                        .param("from", "2026-01-01").param("to", "2026-03-01")
                        .with(authentication(auth("REPORT_HR"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX));
    }

    // ── executive-summary ─────────────────────────────────────────────────────

    @Test
    void executiveSummary_withPermission_returnsPdf() throws Exception {
        doNothing().when(executiveSummaryPdf).write(anyInt(), anyInt(), any());
        mvc.perform(get("/v1/reports/executive-summary")
                        .param("year", "2026").param("month", "5")
                        .with(authentication(auth("REPORT_EXECUTIVE"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", PDF));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private Authentication auth(String... permissions) {
        var principal = new AuthenticatedUser(
                UUID.randomUUID(), "test@hrms.kz", "SUPER_ADMIN", UUID.randomUUID());
        var authorities = List.of(permissions).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}
