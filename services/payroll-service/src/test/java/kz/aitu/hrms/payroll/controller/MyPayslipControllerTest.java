package kz.aitu.hrms.payroll.controller;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.payroll.controller.WebMvcTestSupport.WithMockJwtUser;
import kz.aitu.hrms.payroll.dto.PayslipDtos;
import kz.aitu.hrms.payroll.exception.GlobalExceptionHandler;
import kz.aitu.hrms.payroll.service.PayslipPdfService;
import kz.aitu.hrms.payroll.service.PayslipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MyPayslipController.class)
@Import({WebMvcTestSupport.class, GlobalExceptionHandler.class})
class MyPayslipControllerTest {

    @Autowired private MockMvc mvc;

    @MockBean private PayslipService payslipService;
    @MockBean private PayslipPdfService pdfService;

    @Test
    @WithMockJwtUser(
            authorities = "PAYSLIP_VIEW_OWN",
            employeeId = "11111111-1111-1111-1111-111111111111")
    void list_returnsOwnPayslips() throws Exception {
        when(payslipService.myPayslips(any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(
                        PayslipDtos.Response.builder()
                                .id(UUID.randomUUID())
                                .netSalary(new BigDecimal("250575.00"))
                                .build())));

        mvc.perform(get("/v1/payroll/my-payslips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @WithMockJwtUser(authorities = "PAYROLL_VIEW")
    void list_forbiddenWithoutOwnAuthority() throws Exception {
        // PAYROLL_VIEW is not enough for /my-payslips — must have PAYSLIP_VIEW_OWN
        mvc.perform(get("/v1/payroll/my-payslips"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockJwtUser(authorities = "PAYSLIP_VIEW_OWN")
    void list_returns400WhenAccountHasNoEmployeeProfile() throws Exception {
        when(payslipService.myPayslips(any())).thenThrow(
                new BusinessException("Your account is not linked to an employee profile. Contact HR."));

        mvc.perform(get("/v1/payroll/my-payslips"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("not linked to an employee")));
    }
}