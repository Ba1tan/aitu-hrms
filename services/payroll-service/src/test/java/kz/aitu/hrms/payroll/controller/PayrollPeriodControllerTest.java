package kz.aitu.hrms.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.payroll.batch.PayrollBatchService;
import kz.aitu.hrms.payroll.controller.WebMvcTestSupport.WithMockJwtUser;
import kz.aitu.hrms.payroll.dto.PeriodDtos;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import kz.aitu.hrms.payroll.exception.GlobalExceptionHandler;
import kz.aitu.hrms.payroll.service.PayrollPeriodService;
import kz.aitu.hrms.payroll.service.PayslipGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice. Exercises @PreAuthorize, JSON binding, validation, and the
 * controller's handoff to the service. Service is mocked.
 */
@WebMvcTest(controllers = PayrollPeriodController.class)
@Import({WebMvcTestSupport.class, GlobalExceptionHandler.class})
class PayrollPeriodControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @MockBean private PayrollPeriodService periodService;
    @MockBean private PayslipGenerationService generationService;
    @MockBean private PayrollBatchService batchService;

    // ── /periods POST ─────────────────────────────────────────────────────

    @Test
    void create_unauthenticatedReturns401or403() throws Exception {
        // No security context → Spring returns 401/403 depending on entry-point.
        // GlobalExceptionHandler converts AccessDenied → 403; AuthenticationException → 401.
        mvc.perform(post("/v1/payroll/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":3,\"workingDays\":22}"))
                .andExpect(status().is(403));
        verify(periodService, never()).create(any());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_VIEW")
    void create_forbiddenWithoutProcessAuthority() throws Exception {
        mvc.perform(post("/v1/payroll/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":3,\"workingDays\":22}"))
                .andExpect(status().isForbidden());
        verify(periodService, never()).create(any());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_PROCESS")
    void create_validRequestReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(periodService.create(any())).thenReturn(PeriodDtos.Response.builder()
                .id(id).year(2026).month(3).status(PayrollPeriodStatus.DRAFT).build());

        mvc.perform(post("/v1/payroll/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":3,\"workingDays\":22}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_PROCESS")
    void create_validatesMonthRange() throws Exception {
        mvc.perform(post("/v1/payroll/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":13,\"workingDays\":22}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.month").exists());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_PROCESS")
    void create_duplicateYearMonth_returns400() throws Exception {
        when(periodService.create(any()))
                .thenThrow(new BusinessException("Payroll period already exists for 2026-03"));

        mvc.perform(post("/v1/payroll/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"month\":3,\"workingDays\":22}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("already exists")));
    }

    // ── /periods GET ──────────────────────────────────────────────────────

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_VIEW")
    void list_returnsPaginatedResponse() throws Exception {
        when(periodService.list(any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(
                        PeriodDtos.Response.builder().year(2026).month(2).status(PayrollPeriodStatus.PAID).build(),
                        PeriodDtos.Response.builder().year(2026).month(1).status(PayrollPeriodStatus.LOCKED).build()
                )));

        mvc.perform(get("/v1/payroll/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].year").value(2026));
    }

    // ── /periods/{id}/generate ────────────────────────────────────────────

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_PROCESS")
    void generate_smallCompanyRunsInline() throws Exception {
        UUID periodId = UUID.randomUUID();
        when(generationService.resolveEmployees(any())).thenReturn(List.of());
        when(generationService.shouldUseBatch(eq(0), any())).thenReturn(false);
        when(generationService.generate(eq(periodId), any())).thenReturn(
                PeriodDtos.GenerateResponse.builder()
                        .async(false).generated(0).skipped(0).errors(0)
                        .totalGrossPayout(java.math.BigDecimal.ZERO)
                        .totalNetPayout(java.math.BigDecimal.ZERO)
                        .errorDetails(List.of())
                        .build());

        mvc.perform(post("/v1/payroll/periods/" + periodId + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.async").value(false));
        verify(batchService, never()).startGenerateJob(any());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_PROCESS")
    void generate_largeCompanyDispatchesBatchJob() throws Exception {
        UUID periodId = UUID.randomUUID();
        when(generationService.resolveEmployees(any())).thenReturn(stubEmployees(120));
        when(generationService.shouldUseBatch(eq(120), any())).thenReturn(true);
        when(batchService.startGenerateJob(periodId)).thenReturn(99L);

        mvc.perform(post("/v1/payroll/periods/" + periodId + "/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.async").value(true))
                .andExpect(jsonPath("$.data.jobId").value(99));
        verify(generationService, never()).generate(any(), any());
        verify(batchService, times(1)).startGenerateJob(periodId);
    }

    // ── status transitions ────────────────────────────────────────────────

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_APPROVE")
    void approve_okWhenAuthorityGranted() throws Exception {
        UUID id = UUID.randomUUID();
        when(periodService.approve(id)).thenReturn(PeriodDtos.Response.builder()
                .id(id).status(PayrollPeriodStatus.APPROVED).build());

        mvc.perform(post("/v1/payroll/periods/" + id + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_VIEW")
    void approve_forbiddenWhenAuthorityMissing() throws Exception {
        mvc.perform(post("/v1/payroll/periods/" + UUID.randomUUID() + "/approve"))
                .andExpect(status().isForbidden());
        verify(periodService, never()).approve(any());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_APPROVE")
    void lock_forbiddenWithoutSystemSettings() throws Exception {
        mvc.perform(post("/v1/payroll/periods/" + UUID.randomUUID() + "/lock"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockJwtUser(authorities = "SYSTEM_SETTINGS")
    void lock_okWhenSystemSettingsGranted() throws Exception {
        UUID id = UUID.randomUUID();
        when(periodService.lock(id)).thenReturn(PeriodDtos.Response.builder()
                .id(id).status(PayrollPeriodStatus.LOCKED).build());

        mvc.perform(post("/v1/payroll/periods/" + id + "/lock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    private static List<kz.aitu.hrms.payroll.client.EmployeeClient.EmployeeSummary> stubEmployees(int n) {
        return java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> new kz.aitu.hrms.payroll.client.EmployeeClient.EmployeeSummary(
                        UUID.randomUUID(), "EMP-" + i, "Name " + i, "e" + i + "@x.kz",
                        "Eng", "Dev", "ACTIVE"))
                .toList();
    }
}