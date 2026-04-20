package kz.aitu.hrms.employee.repository;

import kz.aitu.hrms.employee.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, UUID> {

    List<EmergencyContact> findAllByEmployee_IdAndDeletedFalseOrderByPrimaryDescCreatedAtAsc(UUID employeeId);

    Optional<EmergencyContact> findByIdAndEmployee_IdAndDeletedFalse(UUID id, UUID employeeId);
}