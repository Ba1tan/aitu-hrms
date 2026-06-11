package kz.aitu.hrms.integration.service;

import kz.aitu.hrms.common.exception.ResourceNotFoundException;
import kz.aitu.hrms.common.security.AuthenticatedUser;
import kz.aitu.hrms.integration.domain.CompanySetting;
import kz.aitu.hrms.integration.domain.SettingsAudit;
import kz.aitu.hrms.integration.dto.settings.SetupStatusDto;
import kz.aitu.hrms.integration.exception.SetupIncompleteException;
import kz.aitu.hrms.integration.repository.CompanySettingRepository;
import kz.aitu.hrms.integration.repository.SettingsAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SetupService {

    private static final Set<String> REQUIRED_KEYS = Set.of(
            "company.name", "company.bin", "company.legal_address",
            "company.timezone", "company.currency", "company.locale_default",
            "company.tax_resident", "attendance.check_in_methods",
            "attendance.work_schedule_default_id"
    );

    private final CompanySettingRepository repo;
    private final SettingsAuditRepository auditRepo;
    private final AuditPublisher auditPublisher;

    @Transactional(readOnly = true)
    public SetupStatusDto getStatus() {
        List<String> missing = REQUIRED_KEYS.stream()
                .filter(key -> repo.findByKey(key).isEmpty()
                        || repo.findByKey(key).get().getValue().isBlank())
                .sorted()
                .toList();

        boolean explicitlyCompleted = repo.findByKey("setup.completed")
                .map(s -> "true".equalsIgnoreCase(s.getValue()))
                .orElse(false);

        boolean configured = missing.isEmpty() && explicitlyCompleted;

        return new SetupStatusDto(configured, REQUIRED_KEYS.size(), missing, explicitlyCompleted);
    }

    @Transactional
    public void complete(AuthenticatedUser actor) {
        SetupStatusDto status = getStatus();
        if (!status.getMissingRequired().isEmpty()) {
            throw new SetupIncompleteException(status.getMissingRequired());
        }
        CompanySetting flag = repo.findByKey("setup.completed")
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", "setup.completed"));
        if ("true".equalsIgnoreCase(flag.getValue())) {
            return;
        }
        flag.setValue("true");
        flag.setUpdatedBy(actor.getEmail());
        repo.save(flag);

        auditRepo.save(SettingsAudit.builder()
                .userId(actor.getUserId())
                .userEmail(actor.getEmail())
                .action("SETUP_COMPLETED")
                .build());

        auditPublisher.audit("SETUP_COMPLETED", "SETTING", null, null,
                java.util.Map.of("key", "setup.completed", "value", "true"));
    }
}
