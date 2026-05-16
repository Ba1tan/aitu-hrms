package kz.aitu.hrms.reporting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.common.security.JwtAuthenticationFilter;
import kz.aitu.hrms.reporting.dto.dashboard.*;
import kz.aitu.hrms.reporting.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DashboardController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
                org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration.class
        })
class DashboardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean DashboardService dashboardService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void configureFilter() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2))
                    .doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void stats_authenticated_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        DashboardStatsDto dto = DashboardStatsDto.builder()
                .employees(EmployeesSectionDto.builder()
                        .totalEmployees(100).activeEmployees(90)
                        .onLeaveEmployees(10).newHiresThisMonth(3).build())
                .personal(PersonalSectionDto.builder()
                        .myLastNetSalary(BigDecimal.valueOf(350000))
                        .myLastPayrollPeriod("Май 2026")
                        .myLeaveBalance(BigDecimal.valueOf(14))
                        .myAttendanceTodayStatus("PRESENT").build())
                .build();

        when(dashboardService.build(any(), any(), any())).thenReturn(dto);

        mvc.perform(get("/v1/dashboard/stats")
                        .with(authentication(mockAuth(userId, "SUPER_ADMIN", empId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employees.totalEmployees").value(100));
    }

    @Test
    void stats_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/v1/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    private Authentication mockAuth(UUID userId, String role, UUID employeeId) {
        var principal = new AuthenticatedUser(userId, "test@hrms.kz", role, employeeId);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority(role)));
    }
}
