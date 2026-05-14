package kz.aitu.hrms.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.notification.domain.NotificationChannel;
import kz.aitu.hrms.notification.domain.NotificationType;
import kz.aitu.hrms.notification.dto.NotificationDto;
import kz.aitu.hrms.notification.dto.ReadAllResponseDto;
import kz.aitu.hrms.notification.dto.UnreadCountDto;
import kz.aitu.hrms.common.jwt.JwtTokenValidator;
import kz.aitu.hrms.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private NotificationService service;
    @MockBean  private JwtTokenValidator jwtTokenValidator;

    private Authentication auth(UUID userId) {
        var principal = new AuthenticatedUser(userId, "test@hrms.kz", "EMPLOYEE", null);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
    }

    private NotificationDto dto(UUID userId) {
        return new NotificationDto(UUID.randomUUID(), "Title", "Msg",
                NotificationType.INFO, NotificationChannel.IN_APP,
                false, null, null, null, LocalDateTime.now());
    }

    @Test
    void getNotifications_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.list(eq(userId), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(dto(userId)), PageRequest.of(0, 20), 1));
        mvc.perform(get("/v1/notifications").with(authentication(auth(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("Title"));
    }

    @Test
    void unreadCount_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.unreadCount(userId)).thenReturn(5L);
        mvc.perform(get("/v1/notifications/unread-count").with(authentication(auth(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(5));
    }

    @Test
    void markRead_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        when(service.markRead(userId, notifId)).thenReturn(dto(userId));
        mvc.perform(put("/v1/notifications/{id}/read", notifId).with(authentication(auth(userId))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void markAllRead_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(service.markAllRead(userId)).thenReturn(3);
        mvc.perform(put("/v1/notifications/read-all").with(authentication(auth(userId))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedCount").value(3));
    }

    @Test
    void delete_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        mvc.perform(delete("/v1/notifications/{id}", notifId).with(authentication(auth(userId))).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getNotifications_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
