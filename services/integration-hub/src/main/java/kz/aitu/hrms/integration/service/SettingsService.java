package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.domain.CompanySetting;
import kz.aitu.hrms.integration.domain.SettingsAudit;
import kz.aitu.hrms.integration.dto.settings.SettingDto;
import kz.aitu.hrms.integration.repository.CompanySettingRepository;
import kz.aitu.hrms.integration.repository.SettingsAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {

    private final CompanySettingRepository repo;
    private final SettingsAuditRepository auditRepo;
    private final TextEncryptor encryptor;

    @Transactional(readOnly = true)
    public List<SettingDto> getAll(String category) {
        List<CompanySetting> settings = category != null
                ? repo.findByCategory(category)
                : repo.findAll();
        return settings.stream().map(this::toPublicDto).toList();
    }

    private SettingDto toPublicDto(CompanySetting s) {
        String displayValue = isEncryptedKey(s.getKey()) ? "********" : s.getValue();
        return new SettingDto(s.getId(), s.getKey(), displayValue,
                s.getDescription(), s.getCategory(), s.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public String get(String key) {
        return repo.findByKey(key)
                .map(s -> isEncryptedKey(key) ? encryptor.decrypt(s.getValue()) : s.getValue())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public String getRequired(String key) {
        return repo.findByKey(key)
                .map(s -> isEncryptedKey(key) ? encryptor.decrypt(s.getValue()) : s.getValue())
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));
    }

    @Transactional(readOnly = true)
    public String getOrDefault(String key, String defaultValue) {
        return repo.findByKey(key)
                .map(s -> isEncryptedKey(key) ? encryptor.decrypt(s.getValue()) : s.getValue())
                .orElse(defaultValue);
    }

    public SettingDto update(String key, String value, AuthenticatedUser actor) {
        CompanySetting setting = repo.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));

        String oldValue = isEncryptedKey(key) ? "REDACTED" : setting.getValue();
        String storedValue = isEncryptedKey(key) ? encryptor.encrypt(value) : value;

        setting.setValue(storedValue);
        setting.setUpdatedBy(actor.getEmail());
        repo.save(setting);

        auditRepo.save(SettingsAudit.builder()
                .userId(actor.getUserId())
                .userEmail(actor.getEmail())
                .action("UPDATE_SETTING")
                .settingKey(key)
                .oldValue(oldValue)
                .newValue(isEncryptedKey(key) ? "REDACTED" : value)
                .build());

        return toPublicDto(setting);
    }

    @Transactional(readOnly = true)
    public CompanySettingSnapshot snapshot() {
        String bin  = getOrDefault("company.bin", "");
        String name = getOrDefault("company.name", "");
        String kbe  = getOrDefault("company.kbe", "17");
        return new CompanySettingSnapshot(bin, name, kbe);
    }

    private boolean isEncryptedKey(String key) {
        return key.endsWith("_password") || key.endsWith("_token")
                || key.endsWith("_secret") || key.equals("integration.1c_password");
    }
}
