package kz.aitu.hrms.payroll.controller;

import kz.aitu.hrms.payroll.controller.WebMvcTestSupport.WithMockJwtUser;
import kz.aitu.hrms.payroll.dto.PayslipDtos;
import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
import kz.aitu.hrms.payroll.exception.GlobalExceptionHandler;
import kz.aitu.hrms.payroll.service.PayslipPdfService;
import kz.aitu.hrms.payroll.service.PayslipService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PayslipController.class)
@Import({WebMvcTestSupport.class, GlobalExceptionHandler.class})
class PayslipControllerTest {

    @Autowired private MockMvc mvc;

    @MockBean private PayslipService payslipService;
    @MockBean private PayslipPdfService pdfService;

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_VIEW")
    void detail_returnsPayslip() throws Exception {
        UUID id = UUID.randomUUID();
        when(payslipService.detail(id)).thenReturn(PayslipDtos.Response.builder()
                .id(id).netSalary(new BigDecimal("250575.00")).status(PayslipStatus.DRAFT).build());

        mvc.perform(get("/v1/payroll/payslips/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.netSalary").value(250575.00));
    }

    @Test
    @WithMockJwtUser(authorities = "PAYSLIP_VIEW_OWN")
    void detail_forbiddenWithoutPayrollView() throws Exception {
        mvc.perform(get("/v1/payroll/payslips/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
        verify(payslipService, never()).detail(any());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYSLIP_ADJUST")
    void adjust_validatesNonNegativeAmounts() throws Exception {
        mvc.perform(patch("/v1/payroll/payslips/" + UUID.randomUUID() + "/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allowances\":-50,\"workedDays\":22}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.allowances").exists());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYSLIP_ADJUST")
    void adjust_validatesWorkedDaysMax() throws Exception {
        mvc.perform(patch("/v1/payroll/payslips/" + UUID.randomUUID() + "/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workedDays\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.workedDays").exists());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYSLIP_ADJUST")
    void adjust_okWithValidPayload() throws Exception {
        UUID id = UUID.randomUUID();
        when(payslipService.adjust(eq(id), any())).thenReturn(PayslipDtos.Response.builder()
                .id(id).status(PayslipStatus.DRAFT)
                .allowances(new BigDecimal("15000")).build());

        mvc.perform(patch("/v1/payroll/payslips/" + id + "/adjust")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allowances\":15000,\"workedDays\":22}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowances").value(15000));
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_APPROVE")
    void approveFlagged_returnsApprovedDraft() throws Exception {
        UUID id = UUID.randomUUID();
        when(payslipService.approveFlagged(id)).thenReturn(PayslipDtos.Response.builder()
                .id(id).status(PayslipStatus.DRAFT).aiReviewed(true).build());

        mvc.perform(post("/v1/payroll/payslips/" + id + "/approve-flagged"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.aiReviewed").value(true));
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_VIEW")
    void pdf_returnsPdfBytesWithAttachmentHeader() throws Exception {
        UUID id = UUID.randomUUID();
        Payslip dummy = new Payslip();
        when(payslipService.requireViewablePayslip(id)).thenReturn(dummy);
        when(pdfService.render(id)).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF

        mvc.perform(get("/v1/payroll/payslips/" + id + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("payslip-" + id + ".pdf")))
                .andExpect(content().bytes(new byte[]{0x25, 0x50, 0x44, 0x46}));
    }
}