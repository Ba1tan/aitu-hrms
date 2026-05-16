package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.domain.CompanySetting;
import kz.aitu.hrms.integration.domain.SettingsAudit;
import kz.aitu.hrms.integration.dto.settings.SettingDto;
import kz.aitu.hrms.integration.repository.CompanySettingRepository;
import kz.aitu.hrms.integration.repository.SettingsAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.encrypt.TextEncryptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock private CompanySettingRepository repo;
    @Mock private SettingsAuditRepository auditRepo;
    @Mock private TextEncryptor encryptor;
    @InjectMocks private SettingsService service;

    private AuthenticatedUser actor;

    @BeforeEach
    void setup() {
        actor = new AuthenticatedUser(UUID.randomUUID(), "admin@hrms.kz", "SUPER_ADMIN", null);
    }

    @Test
    void update_plainKey_storesPlaintext() {
        CompanySetting s = CompanySetting.builder()
                .id(UUID.randomUUID()).key("company.name").value("OldName").category("company").build();
        when(repo.findByKey("company.name")).thenReturn(Optional.of(s));
        when(repo.save(any())).thenReturn(s);
        when(auditRepo.save(any())).thenReturn(null);

        SettingDto result = service.update("company.name", "NewName", actor);

        assertThat(s.getValue()).isEqualTo("NewName");
        assertThat(result.getValue()).isEqualTo("NewName");
        verify(encryptor, never()).encrypt(any());
    }

    @Test
    void update_passwordKey_storesCiphertext_publicDtoReturnsStars() {
        CompanySetting s = CompanySetting.builder()
                .id(UUID.randomUUID()).key("integration.1c_password").value("old_cipher").category("integration").build();
        when(repo.findByKey("integration.1c_password")).thenReturn(Optional.of(s));
        when(encryptor.encrypt("secret123")).thenReturn("CIPHER_TEXT");
        when(repo.save(any())).thenReturn(s);
        when(auditRepo.save(any())).thenReturn(null);

        SettingDto result = service.update("integration.1c_password", "secret123", actor);

        assertThat(s.getValue()).isEqualTo("CIPHER_TEXT");
        assertThat(result.getValue()).isEqualTo("********");
    }

    @Test
    void getRequired_encryptedKey_decryptsBeforeReturn() {
        CompanySetting s = CompanySetting.builder()
                .key("integration.1c_password").value("CIPHER").category("integration").build();
        when(repo.findByKey("integration.1c_password")).thenReturn(Optional.of(s));
        when(encryptor.decrypt("CIPHER")).thenReturn("plaintext");

        String val = service.getRequired("integration.1c_password");

        assertThat(val).isEqualTo("plaintext");
    }

    @Test
    void getAll_passwordKey_returnsStars() {
        CompanySetting s = CompanySetting.builder()
                .id(UUID.randomUUID()).key("integration.1c_password")
                .value("CIPHER").category("integration").build();
        when(repo.findAll()).thenReturn(java.util.List.of(s));

        var dtos = service.getAll(null);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getValue()).isEqualTo("********");
    }

    @Test
    void update_auditsChange() {
        CompanySetting s = CompanySetting.builder()
                .id(UUID.randomUUID()).key("company.bin").value("000000000000").category("company").build();
        when(repo.findByKey("company.bin")).thenReturn(Optional.of(s));
        when(repo.save(any())).thenReturn(s);

        ArgumentCaptor<SettingsAudit> auditCaptor = ArgumentCaptor.forClass(SettingsAudit.class);
        when(auditRepo.save(auditCaptor.capture())).thenReturn(null);

        service.update("company.bin", "123456789012", actor);

        SettingsAudit audit = auditCaptor.getValue();
        assertThat(audit.getAction()).isEqualTo("UPDATE_SETTING");
        assertThat(audit.getSettingKey()).isEqualTo("company.bin");
        assertThat(audit.getOldValue()).isEqualTo("000000000000");
        assertThat(audit.getNewValue()).isEqualTo("123456789012");
        assertThat(audit.getUserId()).isEqualTo(actor.getUserId());
    }

    @Test
    void update_missingKey_throwsNotFound() {
        when(repo.findByKey("nonexistent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update("nonexistent", "val", actor))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRequired_missingKey_throwsNotFound() {
        when(repo.findByKey("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRequired("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
