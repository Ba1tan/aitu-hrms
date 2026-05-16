package kz.aitu.hrms.integration.repository;

import kz.aitu.hrms.integration.domain.SettingsAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettingsAuditRepository extends JpaRepository<SettingsAudit, UUID> {
}
