package kz.aitu.hrms.payroll.controller;

import kz.aitu.hrms.payroll.controller.WebMvcTestSupport.WithMockJwtUser;
import kz.aitu.hrms.payroll.dto.AdditionDtos;
import kz.aitu.hrms.payroll.entity.AdditionCategory;
import kz.aitu.hrms.payroll.entity.AdditionType;
import kz.aitu.hrms.payroll.exception.GlobalExceptionHandler;
import kz.aitu.hrms.payroll.service.PayrollAdditionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PayrollAdditionController.class)
@Import({WebMvcTestSupport.class, GlobalExceptionHandler.class})
class PayrollAdditionControllerTest {

    @Autowired private MockMvc mvc;

    @MockBean private PayrollAdditionService service;

    @Test
    @WithMockJwtUser(authorities = "PAYSLIP_ADJUST")
    void create_validatesRequiredFields() throws Exception {
        // Missing employeeId, periodId, type, category, amount
        mvc.perform(post("/v1/payroll/additions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.employeeId").exists())
                .andExpect(jsonPath("$.errors.periodId").exists())
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.category").exists())
                .andExpect(jsonPath("$.errors.amount").exists());
        verify(service, never()).create(any());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYSLIP_ADJUST")
    void create_acceptsValidPayload() throws Exception {
        UUID empId = UUID.randomUUID();
        UUID periodId = UUID.randomUUID();
        when(service.create(any())).thenReturn(AdditionDtos.Response.builder()
                .id(UUID.randomUUID())
                .employeeId(empId)
                .periodId(periodId)
                .type(AdditionType.BONUS)
                .category(AdditionCategory.BONUS_PERFORMANCE)
                .amount(new BigDecimal("15000"))
                .isTaxable(true)
                .build());

        String body = String.format(
                "{\"employeeId\":\"%s\",\"periodId\":\"%s\",\"type\":\"BONUS\","
                        + "\"category\":\"BONUS_PERFORMANCE\",\"amount\":15000}",
                empId, periodId);

        mvc.perform(post("/v1/payroll/additions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(15000))
                .andExpect(jsonPath("$.data.type").value("BONUS"));
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_VIEW")
    void create_forbiddenWithoutAdjustAuthority() throws Exception {
        // Use a valid payload so validation passes — the @PreAuthorize check is what we're asserting.
        String body = String.format(
                "{\"employeeId\":\"%s\",\"periodId\":\"%s\",\"type\":\"BONUS\","
                        + "\"category\":\"BONUS_PERFORMANCE\",\"amount\":15000}",
                UUID.randomUUID(), UUID.randomUUID());

        mvc.perform(post("/v1/payroll/additions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYSLIP_ADJUST")
    void delete_callsServiceAndReturnsSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(delete("/v1/payroll/additions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Addition deleted"));
        verify(service).delete(id);
    }
}