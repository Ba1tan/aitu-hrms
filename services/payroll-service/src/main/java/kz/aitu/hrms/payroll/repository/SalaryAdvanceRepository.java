package kz.aitu.hrms.payroll.repository;

import kz.aitu.hrms.payroll.entity.AdvanceStatus;
import kz.aitu.hrms.payroll.entity.SalaryAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryAdvanceRepository extends JpaRepository<SalaryAdvance, UUID> {

    Optional<SalaryAdvance> findByIdAndDeletedFalse(UUID id);

    List<SalaryAdvance> findByEmployeeIdAndStatusAndDeletedFalse(UUID employeeId, AdvanceStatus status);

    List<SalaryAdvance> findByEmployeeIdAndDeletedFalse(UUID employeeId);
}