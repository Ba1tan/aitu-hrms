package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.domain.CompanySetting;
import kz.aitu.hrms.integration.dto.settings.SetupStatusDto;
import kz.aitu.hrms.integration.exception.SetupIncompleteException;
import kz.aitu.hrms.integration.repository.CompanySettingRepository;
import kz.aitu.hrms.integration.repository.SettingsAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupServiceTest {

    @Mock private CompanySettingRepository repo;
    @Mock private SettingsAuditRepository auditRepo;
    @Mock private AuditPublisher auditPublisher;
    @InjectMocks private SetupService service;

    private AuthenticatedUser actor;

    @BeforeEach
    void setup() {
        actor = new AuthenticatedUser(UUID.randomUUID(), "admin@hrms.kz", "SUPER_ADMIN", null);
    }

    private void mockKey(String key, String value) {
        when(repo.findByKey(key)).thenReturn(Optional.of(
                CompanySetting.builder().key(key).value(value).category("test").build()));
    }

    private void mockAllRequiredKeys() {
        mockKey("company.name", "ТОО Компания");
        mockKey("company.bin", "123456789012");
        mockKey("company.legal_address", "Алматы, ул. Абая 1");
        mockKey("company.timezone", "Asia/Almaty");
        mockKey("company.currency", "KZT");
        mockKey("company.locale_default", "ru");
        mockKey("company.tax_resident", "true");
        mockKey("attendance.check_in_methods", "[\"WEB\"]");
        mockKey("attendance.work_schedule_default_id", UUID.randomUUID().toString());
    }

    @Test
    void getStatus_missingKey_returnsFalse() {
        when(repo.findByKey(anyString())).thenReturn(Optional.empty());

        SetupStatusDto status = service.getStatus();

        assertThat(status.isConfigured()).isFalse();
        assertThat(status.getMissingRequired()).isNotEmpty();
        assertThat(status.getTotalRequired()).isEqualTo(9);
    }

    @Test
    void getStatus_allRequiredSet_butNotCompleted_returnsFalse() {
        mockAllRequiredKeys();
        when(repo.findByKey("setup.completed")).thenReturn(Optional.of(
                CompanySetting.builder().key("setup.completed").value("false").category("system").build()));

        SetupStatusDto status = service.getStatus();

        assertThat(status.isConfigured()).isFalse();
        assertThat(status.getMissingRequired()).isEmpty();
        assertThat(status.isExplicitlyCompleted()).isFalse();
    }

    @Test
    void getStatus_allRequiredSetAndCompleted_returnsTrue() {
        mockAllRequiredKeys();
        when(repo.findByKey("setup.completed")).thenReturn(Optional.of(
                CompanySetting.builder().key("setup.completed").value("true").category("system").build()));

        SetupStatusDto status = service.getStatus();

        assertThat(status.isConfigured()).isTrue();
        assertThat(status.isExplicitlyCompleted()).isTrue();
    }

    @Test
    void complete_missingKeys_throws409() {
        when(repo.findByKey(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(actor))
                .isInstanceOf(SetupIncompleteException.class)
                .satisfies(e -> {
                    assertThat(((SetupIncompleteException) e).getMissingRequired()).isNotEmpty();
                });
    }

    @Test
    void complete_allKeysPresent_setsCompletedFlag() {
        mockAllRequiredKeys();
        CompanySetting flag = CompanySetting.builder()
                .key("setup.completed").value("false").category("system").build();
        when(repo.findByKey("setup.completed")).thenReturn(Optional.of(flag));
        when(repo.save(any())).thenReturn(flag);
        when(auditRepo.save(any())).thenReturn(null);

        service.complete(actor);

        assertThat(flag.getValue()).isEqualTo("true");
        verify(auditRepo).save(argThat(a -> "SETUP_COMPLETED".equals(a.getAction())));
    }

    @Test
    void complete_alreadyCompleted_isNoOp() {
        mockAllRequiredKeys();
        when(repo.findByKey("setup.completed")).thenReturn(Optional.of(
                CompanySetting.builder().key("setup.completed").value("true").category("system").build()));

        service.complete(actor);

        verify(repo, never()).save(any());
        verify(auditRepo, never()).save(any());
    }
}
