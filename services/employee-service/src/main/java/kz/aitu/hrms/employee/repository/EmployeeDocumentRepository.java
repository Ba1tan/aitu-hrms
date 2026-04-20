package kz.aitu.hrms.employee.repository;

import kz.aitu.hrms.employee.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {

    List<EmployeeDocument> findAllByEmployee_IdAndDeletedFalseOrderByCreatedAtDesc(UUID employeeId);

    Optional<EmployeeDocument> findByIdAndEmployee_IdAndDeletedFalse(UUID id, UUID employeeId);
}