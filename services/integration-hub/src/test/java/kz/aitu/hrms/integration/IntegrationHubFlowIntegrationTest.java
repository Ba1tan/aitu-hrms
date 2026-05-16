package kz.aitu.hrms.integration;

import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.domain.CompanySetting;
import kz.aitu.hrms.integration.event.publisher.IntegrationEventPublisher;
import kz.aitu.hrms.integration.repository.CompanySettingRepository;
import kz.aitu.hrms.integration.repository.SettingsAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IntegrationHubFlowIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private CompanySettingRepository settingRepo;
    @Autowired private SettingsAuditRepository auditRepo;

    @MockBean private ConnectionFactory connectionFactory;
    @MockBean private RabbitTemplate rabbitTemplate;
    @MockBean private IntegrationEventPublisher integrationEventPublisher;

    private Authentication adminAuth;

    @BeforeEach
    void setup() {
        auditRepo.deleteAll();
        settingRepo.deleteAll();

        seedSetting("setup.completed", "false", "system");

        var principal = new AuthenticatedUser(UUID.randomUUID(), "admin@hrms.kz", "SUPER_ADMIN", null);
        adminAuth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("SYSTEM_SETTINGS")));
    }

    private void seedSetting(String key, String value, String category) {
        settingRepo.save(CompanySetting.builder()
                .key(key).value(value).category(category).build());
    }

    @Test
    void completeSetup_missingRequired_returns409() throws Exception {
        mvc.perform(post("/v1/settings/complete-setup")
                        .with(authentication(adminAuth)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.missingRequired").isArray());
    }

    @Test
    void setupStatus_freshInstance_returnsFalse() throws Exception {
        mvc.perform(get("/v1/settings/setup-status")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.totalRequired").value(10));
    }

    @Test
    void fullSetupFlow_setsCompletedAndAudits() throws Exception {
        // Seed all required keys
        seedSetting("company.name", "ТОО Тест", "company");
        seedSetting("company.bin", "123456789012", "company");
        seedSetting("company.legal_address", "Алматы, ул. Абая 1", "company");
        seedSetting("company.timezone", "Asia/Almaty", "company");
        seedSetting("company.currency", "KZT", "company");
        seedSetting("company.locale_default", "ru", "company");
        seedSetting("company.tax_resident", "true", "company");
        seedSetting("attendance.check_in_methods", "[\"WEB\"]", "attendance");
        seedSetting("attendance.require_face", "false", "attendance");
        seedSetting("attendance.work_schedule_default_id", UUID.randomUUID().toString(), "attendance");

        // complete-setup should succeed
        mvc.perform(post("/v1/settings/complete-setup")
                        .with(authentication(adminAuth)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // verify flag flipped
        assertThat(settingRepo.findByKey("setup.completed")
                .map(CompanySetting::getValue).orElse(""))
                .isEqualTo("true");

        // verify audit row written
        assertThat(auditRepo.findAll())
                .anyMatch(a -> "SETUP_COMPLETED".equals(a.getAction()));

        // calling again is idempotent
        mvc.perform(post("/v1/settings/complete-setup")
                        .with(authentication(adminAuth)).with(csrf()))
                .andExpect(status().isOk());
    }
}
