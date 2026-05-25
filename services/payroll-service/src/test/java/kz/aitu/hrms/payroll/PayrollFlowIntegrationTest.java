package kz.aitu.hrms.payroll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import kz.aitu.hrms.payroll.repository.PayrollPeriodRepository;
import kz.aitu.hrms.payroll.repository.PayslipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test of the canonical payroll flow:
 *   create period → generate payslips → adjust one → approve → mark paid → lock.
 *
 * The HTTP layer is exercised via MockMvc; the persistence layer hits H2 via
 * the test profile; cross-service calls (employee-service, attendance-service)
 * are stubbed with WireMock so the test runs hermetically.
 *
 * Spring Batch initialization is disabled in the test profile to keep startup
 * fast — the inline generation path is what matters for correctness.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfig.class, PayrollFlowIntegrationTest.WireMockBootstrap.class})
@TestPropertySource(properties = {
        "spring.cloud.openfeign.client.config.default.connectTimeout=2000",
        "spring.cloud.openfeign.client.config.default.readTimeout=2000",
        "spring.batch.jdbc.initialize-schema=never",
        "app.payroll.batch-threshold=999"
})
@DirtiesContext
class PayrollFlowIntegrationTest {

    private static final UUID HR_USER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ALICE_ID   = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB_ID     = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private PayrollPeriodRepository periodRepo;
    @Autowired private PayslipRepository payslipRepo;
    @Autowired private WireMockServer wireMock;

    private Authentication hrAuth;

    @BeforeEach
    void setUpAuth() {
        AuthenticatedUser principal = new AuthenticatedUser(
                HR_USER_ID, "hr@hrms.kz", "HR_MANAGER", null);
        var authorities = List.of(
                new SimpleGrantedAuthority("PAYROLL_PROCESS"),
                new SimpleGrantedAuthority("PAYROLL_VIEW"),
                new SimpleGrantedAuthority("PAYROLL_APPROVE"),
                new SimpleGrantedAuthority("PAYROLL_PAY"),
                new SimpleGrantedAuthority("PAYSLIP_ADJUST"),
                new SimpleGrantedAuthority("SYSTEM_SETTINGS"));
        hrAuth = new UsernamePasswordAuthenticationToken(principal, null, authorities);

        wireMock.resetAll();
        stubEmployeeService();
        stubAttendanceService();
    }

    @AfterEach
    void clearDb() {
        payslipRepo.deleteAll();
        periodRepo.deleteAll();
    }

    @Test
    void fullPayrollFlow_fromCreateToLock() throws Exception {
        // 1) create period — DRAFT
        MvcResult created = mvc.perform(post("/v1/payroll/periods")
                        .with(authentication(hrAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":3,\"workingDays\":22}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        UUID periodId = UUID.fromString(
                json.readTree(created.getResponse().getContentAsString())
                        .at("/data/id").asText());

        // 2) generate payslips inline (force sync via batch-threshold=999)
        mvc.perform(post("/v1/payroll/periods/" + periodId + "/generate")
                        .with(authentication(hrAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.async").value(false))
                .andExpect(jsonPath("$.data.generated").value(2))
                .andExpect(jsonPath("$.data.errors").value(0));

        PayrollPeriod period = periodRepo.findById(periodId).orElseThrow();
        assertThat(period.getStatus()).isEqualTo(PayrollPeriodStatus.COMPLETED);
        assertThat(payslipRepo.findByPeriodIdAndDeletedFalse(periodId)).hasSize(2);

        // 3) sanity-check the math for Alice (300 000 ₸ resident, full month)
        Payslip alice = payslipRepo.findByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, ALICE_ID).orElseThrow();
        assertThat(alice.getOpvAmount()).isEqualByComparingTo("30000.00");
        assertThat(alice.getVosmsAmount()).isEqualByComparingTo("6000.00");
        assertThat(alice.getIpnAmount()).isEqualByComparingTo("13425.00");
        assertThat(alice.getNetSalary()).isEqualByComparingTo("250575.00");
        assertThat(alice.getStatus()).isEqualTo(PayslipStatus.DRAFT);

        // 4) adjust Bob's payslip (add 25 000 bonus)
        Payslip bob = payslipRepo.findByPeriodIdAndEmployeeIdAndDeletedFalse(periodId, BOB_ID).orElseThrow();
        BigDecimal netBefore = bob.getNetSalary();
        mvc.perform(patch("/v1/payroll/payslips/" + bob.getId() + "/adjust")
                        .with(authentication(hrAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allowances\":25000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowances").value(25000));
        Payslip bobAfter = payslipRepo.findById(bob.getId()).orElseThrow();
        assertThat(bobAfter.getNetSalary()).isEqualByComparingTo(netBefore.add(new BigDecimal("25000.00")));

        // 5) approve period — should move period to APPROVED, payslips to APPROVED
        mvc.perform(post("/v1/payroll/periods/" + periodId + "/approve")
                        .with(authentication(hrAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        assertThat(payslipRepo.findByPeriodIdAndDeletedFalse(periodId))
                .allMatch(p -> p.getStatus() == PayslipStatus.APPROVED);

        // 6) mark paid
        mvc.perform(post("/v1/payroll/periods/" + periodId + "/mark-paid")
                        .with(authentication(hrAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
        assertThat(payslipRepo.findByPeriodIdAndDeletedFalse(periodId))
                .allMatch(p -> p.getStatus() == PayslipStatus.PAID);

        // 7) lock — only allowed from PAID
        mvc.perform(post("/v1/payroll/periods/" + periodId + "/lock")
                        .with(authentication(hrAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    void cannotApproveBeforeGenerating() throws Exception {
        MvcResult res = mvc.perform(post("/v1/payroll/periods")
                        .with(authentication(hrAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":4,\"workingDays\":22}"))
                .andReturn();
        UUID periodId = UUID.fromString(json.readTree(res.getResponse().getContentAsString())
                .at("/data/id").asText());

        mvc.perform(post("/v1/payroll/periods/" + periodId + "/approve")
                        .with(authentication(hrAuth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("COMPLETED")));
    }

    @Test
    void duplicateYearMonth_isRejected() throws Exception {
        mvc.perform(post("/v1/payroll/periods")
                        .with(authentication(hrAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":5,\"workingDays\":22}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/v1/payroll/periods")
                        .with(authentication(hrAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":5,\"workingDays\":22}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("already exists")));
    }

    // ── WireMock stubs ────────────────────────────────────────────────────

    private void stubEmployeeService() {
        wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/api/v1/employees"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "success": true,
                                  "message": "Success",
                                  "data": {
                                    "content": [
                                      {"id":"%s","employeeNumber":"EMP-001","fullName":"Иванова Алиса",
                                       "email":"alice@hrms.kz","department":"Engineering","position":"Senior",
                                       "status":"ACTIVE"},
                                      {"id":"%s","employeeNumber":"EMP-002","fullName":"Петров Боб",
                                       "email":"bob@hrms.kz","department":"Engineering","position":"Junior",
                                       "status":"ACTIVE"}
                                    ],
                                    "totalElements": 2
                                  }
                                }
                                """.formatted(ALICE_ID, BOB_ID))));

        wireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/employees/" + ALICE_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "success": true,
                                  "data": {
                                    "id":"%s","employeeNumber":"EMP-001","firstName":"Алиса","lastName":"Иванова",
                                    "fullName":"Иванова Алиса","email":"alice@hrms.kz","iin":"880101400123",
                                    "baseSalary":"300000.00","disabilityGroup":"NONE",
                                    "isResident":true,"isPensioner":false,
                                    "department":{"id":"%s","name":"Engineering"},
                                    "position":{"id":"%s","title":"Senior"}
                                  }
                                }
                                """.formatted(ALICE_ID, UUID.randomUUID(), UUID.randomUUID()))));

        wireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/api/v1/employees/" + BOB_ID))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "success": true,
                                  "data": {
                                    "id":"%s","employeeNumber":"EMP-002","firstName":"Боб","lastName":"Петров",
                                    "fullName":"Петров Боб","email":"bob@hrms.kz","iin":"900202400456",
                                    "baseSalary":"180000.00","disabilityGroup":"NONE",
                                    "isResident":true,"isPensioner":false,
                                    "department":{"id":"%s","name":"Engineering"},
                                    "position":{"id":"%s","title":"Junior"}
                                  }
                                }
                                """.formatted(BOB_ID, UUID.randomUUID(), UUID.randomUUID()))));
    }

    private void stubAttendanceService() {
        wireMock.stubFor(WireMock.get(WireMock.urlMatching("/api/v1/attendance/summary/employee/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "success": true,
                                  "data": {
                                    "employeeId":"00000000-0000-0000-0000-000000000000",
                                    "year":2026,"month":3,
                                    "presentDays":22,"lateDays":0,"absentDays":0,"halfDays":0,
                                    "onLeaveDays":0,"holidayDays":0,
                                    "totalWorkedHours":"176.00","overtimeHours":"0.00"
                                  }
                                }
                                """)));
    }

    // ── WireMock lifecycle + dynamic property wiring ──────────────────────

    @TestConfiguration
    static class WireMockBootstrap {
        @Bean(destroyMethod = "stop")
        @Primary
        public WireMockServer wireMockServer() {
            WireMockServer server = new WireMockServer(
                    WireMockConfiguration.options().port(WIREMOCK_PORT));
            server.start();
            return server;
        }
    }

    private static final int WIREMOCK_PORT = findFreePort();

    @DynamicPropertySource
    static void overrideServiceUris(DynamicPropertyRegistry registry) {
        String base = "http://localhost:" + WIREMOCK_PORT;
        registry.add("app.services.employee-service-uri",   () -> base);
        registry.add("app.services.attendance-service-uri", () -> base);
        registry.add("app.services.leave-service-uri",      () -> base);
    }

    private static int findFreePort() {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot reserve a free port for WireMock", e);
        }
    }
}