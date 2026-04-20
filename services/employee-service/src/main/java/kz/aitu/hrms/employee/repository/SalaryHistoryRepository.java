package kz.aitu.hrms.employee.repository;

import kz.aitu.hrms.employee.entity.SalaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalaryHistoryRepository extends JpaRepository<SalaryHistory, UUID> {

    List<SalaryHistory> findAllByEmployee_IdAndDeletedFalseOrderByEffectiveDateDesc(UUID employeeId);
}