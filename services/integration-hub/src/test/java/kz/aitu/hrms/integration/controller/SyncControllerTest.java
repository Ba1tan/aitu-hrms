package kz.aitu.hrms.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import kz.aitu.hrms.integration.dto.sync.SyncJobDto;
import kz.aitu.hrms.integration.dto.sync.SyncTriggerResponseDto;
import kz.aitu.hrms.integration.service.SyncJobService;
import kz.aitu.hrms.integration.service.SyncOrchestrator;
import kz.aitu.hrms.common.jwt.JwtTokenValidator;
import kz.aitu.hrms.integration.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

@WebMvcTest(SyncController.class)
@Import(SecurityConfig.class)
class SyncControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private SyncOrchestrator syncOrchestrator;
    @MockBean  private SyncJobService syncJobService;
    @MockBean  private JwtTokenValidator jwtTokenValidator;

    private Authentication auth(String... authorities) {
        var principal = new AuthenticatedUser(UUID.randomUUID(), "admin@hrms.kz", "SUPER_ADMIN", null);
        List<SimpleGrantedAuthority> auths = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, auths);
    }

    @Test
    void triggerSync_returns202() throws Exception {
        UUID periodId = UUID.randomUUID();
        SyncTriggerResponseDto resp = SyncTriggerResponseDto.builder()
                .jobId(UUID.randomUUID()).periodId(periodId)
                .target(SyncTarget.ONE_C).status(SyncStatus.IN_PROGRESS).build();
        when(syncOrchestrator.trigger(eq(periodId), any())).thenReturn(resp);

        mvc.perform(post("/v1/integration/sync/{periodId}", periodId)
                        .with(authentication(auth("SYSTEM_SETTINGS"))).with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void triggerSync_noAuth_returns401() throws Exception {
        mvc.perform(post("/v1/integration/sync/{periodId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void triggerSync_wrongAuthority_returns403() throws Exception {
        mvc.perform(post("/v1/integration/sync/{periodId}", UUID.randomUUID())
                        .with(authentication(auth("EMPLOYEE"))).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void status_returns200() throws Exception {
        UUID jobId = UUID.randomUUID();
        SyncJobDto dto = SyncJobDto.builder().id(jobId).status(SyncStatus.SUCCESS).build();
        when(syncJobService.getById(jobId)).thenReturn(dto);

        mvc.perform(get("/v1/integration/sync/status/{jobId}", jobId)
                        .with(authentication(auth("INTEGRATION_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }

    @Test
    void history_returns200() throws Exception {
        when(syncJobService.list(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/v1/integration/sync/history")
                        .with(authentication(auth("SYSTEM_SETTINGS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void retry_returns200() throws Exception {
        UUID jobId = UUID.randomUUID();
        SyncJobDto dto = SyncJobDto.builder().id(jobId).status(SyncStatus.IN_PROGRESS).build();
        when(syncOrchestrator.retryJob(eq(jobId), any())).thenReturn(dto);

        mvc.perform(post("/v1/integration/retry/{jobId}", jobId)
                        .with(authentication(auth("SYSTEM_SETTINGS"))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(jobId.toString()));
    }
}
