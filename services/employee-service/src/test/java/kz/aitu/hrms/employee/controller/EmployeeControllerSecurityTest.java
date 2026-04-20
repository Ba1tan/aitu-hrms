package kz.aitu.hrms.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.employee.TestConfig;
import kz.aitu.hrms.employee.dto.EmployeeDtos;
import kz.aitu.hrms.employee.service.EmployeeImportExportService;
import kz.aitu.hrms.employee.service.EmployeeService;
import kz.aitu.hrms.employee.service.OrgChartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
class EmployeeControllerSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean EmployeeService employeeService;
    @MockBean OrgChartService orgChartService;
    @MockBean EmployeeImportExportService importExportService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mvc.perform(get("/v1/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCanListBecauseScopingHappensInService() throws Exception {
        when(employeeService.list(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/v1/employees"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotCreateEmployee() throws Exception {
        EmployeeDtos.CreateEmployeeRequest req = validCreateRequest();
        mvc.perform(post("/v1/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void hrManagerCanCreateEmployee() throws Exception {
        when(employeeService.create(any())).thenReturn(
                EmployeeDtos.EmployeeResponse.builder()
                        .id(UUID.randomUUID())
                        .employeeNumber("EMP-202604-001")
                        .fullName("Nursultan Aslan")
                        .build());
        EmployeeDtos.CreateEmployeeRequest req = validCreateRequest();
        mvc.perform(post("/v1/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP-202604-001"));
    }

    @Test
    @WithMockUser(roles = "HR_SPECIALIST")
    void hrSpecialistCanCreateEmployee() throws Exception {
        when(employeeService.create(any())).thenReturn(
                EmployeeDtos.EmployeeResponse.builder()
                        .id(UUID.randomUUID())
                        .employeeNumber("EMP-202604-002")
                        .build());
        mvc.perform(post("/v1/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void accountantCannotCreateEmployee() throws Exception {
        mvc.perform(post("/v1/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void rejectsInvalidIinShapeBeforeHittingService() throws Exception {
        EmployeeDtos.CreateEmployeeRequest req = validCreateRequest();
        req.setIin("abc");
        mvc.perform(post("/v1/employees")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void hrManagerCanDeleteEmployee() throws Exception {
        mvc.perform(delete("/v1/employees/11111111-1111-1111-1111-111111111111")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR_SPECIALIST")
    void hrSpecialistCannotDeleteEmployee() throws Exception {
        mvc.perform(delete("/v1/employees/11111111-1111-1111-1111-111111111111")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdminCanDeleteEmployee() throws Exception {
        mvc.perform(delete("/v1/employees/11111111-1111-1111-1111-111111111111")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotExport() throws Exception {
        mvc.perform(get("/v1/employees/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DIRECTOR")
    void directorCanExport() throws Exception {
        when(importExportService.exportToXlsx())
                .thenReturn(new java.io.ByteArrayInputStream(new byte[0]));
        mvc.perform(get("/v1/employees/export"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ACCOUNTANT")
    void accountantCannotExport() throws Exception {
        mvc.perform(get("/v1/employees/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void anyoneAuthenticatedCanViewOrgChart() throws Exception {
        when(orgChartService.fullChart()).thenReturn(List.of());
        mvc.perform(get("/v1/employees/org-chart"))
                .andExpect(status().isOk());
    }

    private EmployeeDtos.CreateEmployeeRequest validCreateRequest() {
        EmployeeDtos.CreateEmployeeRequest req = new EmployeeDtos.CreateEmployeeRequest();
        req.setFirstName("Aslan");
        req.setLastName("Nursultan");
        req.setEmail("aslan@example.com");
        req.setIin("910915300123");
        req.setHireDate(LocalDate.now());
        req.setBaseSalary(new BigDecimal("500000"));
        return req;
    }
}