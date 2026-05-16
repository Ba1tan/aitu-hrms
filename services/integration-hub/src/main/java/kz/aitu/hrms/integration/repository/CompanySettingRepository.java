package kz.aitu.hrms.integration.repository;

import kz.aitu.hrms.integration.domain.CompanySetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanySettingRepository extends JpaRepository<CompanySetting, UUID> {

    Optional<CompanySetting> findByKey(String key);

    List<CompanySetting> findByCategory(String category);
}
