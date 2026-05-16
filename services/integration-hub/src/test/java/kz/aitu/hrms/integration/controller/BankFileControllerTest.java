package kz.aitu.hrms.integration.controller;

import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.service.BankFileService;
import kz.aitu.hrms.integration.service.SettingsService;
import kz.aitu.hrms.common.jwt.JwtTokenValidator;
import kz.aitu.hrms.integration.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankFileController.class)
@Import(SecurityConfig.class)
class BankFileControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean  private BankFileService bankFileService;
    @MockBean  private SettingsService settingsService;
    @MockBean  private JwtTokenValidator jwtTokenValidator;

    private Authentication auth(String... authorities) {
        var principal = new AuthenticatedUser(UUID.randomUUID(), "admin@hrms.kz", "SUPER_ADMIN", null);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority(authorities[0])));
    }

    @Test
    void downloadBankFile_returns200_withCorrectHeaders() throws Exception {
        UUID periodId = UUID.randomUUID();
        when(settingsService.getOrDefault("integration.bank_default_format", "KASPI_TSV"))
                .thenReturn("KASPI_TSV");
        doNothing().when(bankFileService).generate(eq(periodId), eq("KASPI_TSV"), any(OutputStream.class));

        mvc.perform(get("/v1/integration/bank-file/{periodId}", periodId)
                        .with(authentication(auth("SYSTEM_SETTINGS"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/octet-stream"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"payment-" + periodId + ".tsv\""));
    }

    @Test
    void downloadBankFile_noAuth_returns401() throws Exception {
        mvc.perform(get("/v1/integration/bank-file/{periodId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
