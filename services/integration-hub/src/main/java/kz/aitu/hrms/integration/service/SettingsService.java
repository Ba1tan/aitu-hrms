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

    /**
     * Non-sensitive keys that any authenticated user can read — used by the
     * frontend's AttendanceWidget (needs check-in methods + work hours) and
     * NotificationsPreferences (needs to know if SMS is wired). DO NOT add
     * keys here without checking they're safe to leak to every employee.
     */
    @Transactional(readOnly = true)
    public List<SettingDto> getPublic() {
        return repo.findAll().stream()
                .filter(s -> isPublicKey(s.getKey()))
                .map(this::toPublicDto)
                .toList();
    }

    private boolean isPublicKey(String key) {
        if (isEncryptedKey(key)) return false;
        // company.* is generally safe — name, bin (BIN is public-record),
        // timezone, locale_default, currency, tax_resident.
        if (key.startsWith("company.")) return true;
        return key.equals("attendance.check_in_methods")
                || key.equals("attendance.require_face")
                || key.equals("attendance.work_schedule_default_id")
                || key.equals("notification.sms_provider")
                || key.equals("payroll.payslip_release_day");
    }

    private SettingDto toPublicDto(CompanySetting s) {
        String displayValue = isEncryptedKey(s.getKey()) ? "********" : s.getValue();
        return new SettingDto(s.getId(), s.getKey(), displayValue,
                s.getDescription(), s.getCategory(), s.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public String get(String key) {
        return repo.findByKey(key)
                .map(this::reveal)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public String getRequired(String key) {
        return repo.findByKey(key)
                .map(this::reveal)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));
    }

    @Transactional(readOnly = true)
    public String getOrDefault(String key, String defaultValue) {
        return repo.findByKey(key)
                .map(this::reveal)
                .orElse(defaultValue);
    }

    // A blank stored value can never be ciphertext (e.g. a seeded-but-unset
    // encrypted key), so return it as-is rather than feeding it to decrypt().
    private String reveal(CompanySetting s) {
        if (isEncryptedKey(s.getKey()) && !s.getValue().isBlank()) {
            return encryptor.decrypt(s.getValue());
        }
        return s.getValue();
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
