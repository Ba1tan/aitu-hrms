package kz.aitu.hrms.employee.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.employee.TestConfig;
import kz.aitu.hrms.employee.dto.DepartmentDtos;
import kz.aitu.hrms.employee.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
class DepartmentControllerSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean DepartmentService departmentService;

    @Test
    void listRequiresAuthentication() throws Exception {
        mvc.perform(get("/v1/departments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void anyAuthenticatedUserCanListDepartments() throws Exception {
        when(departmentService.list()).thenReturn(List.of());
        mvc.perform(get("/v1/departments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void employeeCannotCreateDepartment() throws Exception {
        DepartmentDtos.CreateDepartmentRequest req = new DepartmentDtos.CreateDepartmentRequest();
        req.setName("Engineering");
        mvc.perform(post("/v1/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void hrManagerCanCreateDepartment() throws Exception {
        when(departmentService.create(any())).thenReturn(
                DepartmentDtos.DepartmentResponse.builder().name("Engineering").build());
        DepartmentDtos.CreateDepartmentRequest req = new DepartmentDtos.CreateDepartmentRequest();
        req.setName("Engineering");
        mvc.perform(post("/v1/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "HR_SPECIALIST")
    void hrSpecialistCanCreateDepartment() throws Exception {
        when(departmentService.create(any())).thenReturn(
                DepartmentDtos.DepartmentResponse.builder().name("Engineering").build());
        DepartmentDtos.CreateDepartmentRequest req = new DepartmentDtos.CreateDepartmentRequest();
        req.setName("Engineering");
        mvc.perform(post("/v1/departments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "HR_MANAGER")
    void hrManagerCanDeleteDepartment() throws Exception {
        mvc.perform(delete("/v1/departments/11111111-1111-1111-1111-111111111111")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR_SPECIALIST")
    void hrSpecialistCannotDeleteDepartment() throws Exception {
        mvc.perform(delete("/v1/departments/11111111-1111-1111-1111-111111111111")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void superAdminCanDeleteDepartment() throws Exception {
        mvc.perform(delete("/v1/departments/11111111-1111-1111-1111-111111111111")
                        .with(csrf()))
                .andExpect(status().isOk());
    }
}