package kz.aitu.hrms.employee.repository;

import kz.aitu.hrms.employee.entity.BiometricData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BiometricDataRepository extends JpaRepository<BiometricData, UUID> {

    Optional<BiometricData> findByEmployee_IdAndDeletedFalse(UUID employeeId);
}