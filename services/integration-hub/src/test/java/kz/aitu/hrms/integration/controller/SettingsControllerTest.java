package kz.aitu.hrms.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.dto.settings.SettingDto;
import kz.aitu.hrms.integration.dto.settings.SettingUpdateRequest;
import kz.aitu.hrms.integration.dto.settings.SetupStatusDto;
import kz.aitu.hrms.integration.service.SetupService;
import kz.aitu.hrms.integration.service.SettingsService;
import kz.aitu.hrms.common.jwt.JwtTokenValidator;
import kz.aitu.hrms.integration.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingsController.class)
@Import(SecurityConfig.class)
class SettingsControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private SettingsService settingsService;
    @MockBean  private SetupService setupService;
    @MockBean  private JwtTokenValidator jwtTokenValidator;

    private Authentication auth(String... authorities) {
        var principal = new AuthenticatedUser(UUID.randomUUID(), "admin@hrms.kz", "SUPER_ADMIN", null);
        List<SimpleGrantedAuthority> auths = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, auths);
    }

    @Test
    void getAll_requiresSystemSettings() throws Exception {
        when(settingsService.getAll(null)).thenReturn(List.of());
        mvc.perform(get("/v1/settings").with(authentication(auth("SYSTEM_SETTINGS"))))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_withoutSystemSettings_returns403() throws Exception {
        mvc.perform(get("/v1/settings").with(authentication(auth("EMPLOYEE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_passwordKey_returns_stars() throws Exception {
        SettingDto dto = new SettingDto(UUID.randomUUID(), "integration.1c_password",
                "********", null, "integration", null);
        when(settingsService.update(eq("integration.1c_password"), any(), any())).thenReturn(dto);

        SettingUpdateRequest req = new SettingUpdateRequest();
        req.setValue("secret123");

        mvc.perform(put("/v1/settings/{key}", "integration.1c_password")
                        .with(authentication(auth("SYSTEM_SETTINGS")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("********"));
    }

    @Test
    void setupStatus_accessibleByAnyAuthenticatedUser() throws Exception {
        SetupStatusDto status = new SetupStatusDto(false, 10, List.of("company.bin"), false);
        when(setupService.getStatus()).thenReturn(status);

        mvc.perform(get("/v1/settings/setup-status").with(authentication(auth("EMPLOYEE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.totalRequired").value(10));
    }

    @Test
    void setupStatus_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/v1/settings/setup-status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void completeSetup_requiresSystemSettings() throws Exception {
        mvc.perform(post("/v1/settings/complete-setup")
                        .with(authentication(auth("EMPLOYEE"))).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
