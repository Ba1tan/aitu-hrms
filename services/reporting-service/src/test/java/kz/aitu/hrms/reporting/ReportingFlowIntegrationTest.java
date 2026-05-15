package kz.aitu.hrms.reporting;

import com.github.tomakehurst.wiremock.client.WireMock;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.common.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 9999)
class ReportingFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setup() throws Exception {
        WireMock.reset();
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    // ── SUPER_ADMIN — все секции заполнены ───────────────────────────────────

    @Test
    void superAdmin_allSectionsPopulated() throws Exception {
        UUID periodId = UUID.randomUUID();
        stubAllUpstreams(periodId);

        mvc.perform(get("/v1/dashboard/stats")
                        .with(authentication(auth("SUPER_ADMIN", UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employees.totalEmployees").value(42))
                .andExpect(jsonPath("$.data.lastPayroll.lastPayrollPeriodName").value("Май 2026"))
                .andExpect(jsonPath("$.data.leave.pendingLeaveRequests").value(3))
                .andExpect(jsonPath("$.data.attendanceToday.todayPresent").value(80))
                .andExpect(jsonPath("$.data.personal.myAttendanceTodayStatus").value("PRESENT"));
    }

    // ── EMPLOYEE — только personal ────────────────────────────────────────────

    @Test
    void employee_onlyPersonalSection() throws Exception {
        UUID empId = UUID.randomUUID();
        stubPersonalUpstreams();

        mvc.perform(get("/v1/dashboard/stats")
                        .with(authentication(auth("EMPLOYEE", empId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employees").doesNotExist())
                .andExpect(jsonPath("$.data.lastPayroll").doesNotExist())
                .andExpect(jsonPath("$.data.leave").doesNotExist())
                .andExpect(jsonPath("$.data.attendanceToday").doesNotExist())
                .andExpect(jsonPath("$.data.personal").exists());
    }

    // ── Деградация: employee-service вернул 503 ───────────────────────────────

    @Test
    void employeeServiceDown_employeesSectionNull_othersOk() throws Exception {
        UUID periodId = UUID.randomUUID();

        // employee count → 503
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/employees/count"))
                .willReturn(aResponse().withStatus(503)));
        // employee list fallback → 503 тоже
        stubFor(WireMock.get(urlPathMatching("/api/v1/employees.*"))
                .willReturn(aResponse().withStatus(503)));

        stubPayroll(periodId);
        stubLeave();
        stubAttendance();
        stubPersonalUpstreams();

        mvc.perform(get("/v1/dashboard/stats")
                        .with(authentication(auth("SUPER_ADMIN", UUID.randomUUID()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employees").doesNotExist())
                .andExpect(jsonPath("$.data.lastPayroll.lastPayrollPeriodName").value("Май 2026"))
                .andExpect(jsonPath("$.data.leave.pendingLeaveRequests").value(3));
    }

    // ── 401 без токена ────────────────────────────────────────────────────────

    @Test
    void unauthenticated_returns401() throws Exception {
        mvc.perform(get("/v1/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    // ── Stub helpers ──────────────────────────────────────────────────────────

    private void stubAllUpstreams(UUID periodId) {
        stubEmployees();
        stubPayroll(periodId);
        stubLeave();
        stubAttendance();
        stubPersonalUpstreams();
    }

    private void stubEmployees() {
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/employees/count"))
                .willReturn(okJson("""
                        {"total":42,"active":38,"onLeave":4,"newHiresThisMonth":2}
                        """)));
    }

    private void stubPayroll(UUID periodId) {
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/payroll/periods/latest"))
                .willReturn(okJson("""
                        {"id":"%s","name":"Май 2026","status":"CLOSED","locked":false}
                        """.formatted(periodId))));

        stubFor(WireMock.get(urlPathMatching("/api/v1/payroll/periods/.*/totals"))
                .willReturn(okJson("""
                        {"totalGross":5000000,"totalNet":4200000,"employeeCount":38}
                        """)));

        stubFor(WireMock.get(urlPathEqualTo("/api/v1/payroll/my-payslips"))
                .willReturn(okJson("""
                        [{"netSalary":350000,"periodName":"Май 2026"}]
                        """)));
    }

    private void stubLeave() {
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/leave/requests/pending/count"))
                .willReturn(okJson("""
                        {"count":3}
                        """)));

        stubFor(WireMock.get(urlPathEqualTo("/api/v1/leave/balances"))
                .willReturn(okJson("""
                        [{"remainingDays":14,"leaveType":"ANNUAL"}]
                        """)));
    }

    private void stubAttendance() {
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/attendance/summary/company"))
                .willReturn(okJson("""
                        {"present":80,"absent":10,"late":10,"total":100}
                        """)));

        stubFor(WireMock.get(urlPathEqualTo("/api/v1/attendance/today"))
                .willReturn(okJson("""
                        {"status":"PRESENT"}
                        """)));
    }

    private void stubPersonalUpstreams() {
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/payroll/my-payslips"))
                .willReturn(okJson("""
                        [{"netSalary":350000,"periodName":"Май 2026"}]
                        """)));
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/leave/balances"))
                .willReturn(okJson("""
                        [{"remainingDays":14,"leaveType":"ANNUAL"}]
                        """)));
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/attendance/today"))
                .willReturn(okJson("""
                        {"status":"PRESENT"}
                        """)));
    }

    private Authentication auth(String role, UUID employeeId) {
        var principal = new AuthenticatedUser(UUID.randomUUID(), "test@hrms.kz", role, employeeId);
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(role)));
    }
}
