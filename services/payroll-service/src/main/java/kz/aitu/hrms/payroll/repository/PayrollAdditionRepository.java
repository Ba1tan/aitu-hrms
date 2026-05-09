package kz.aitu.hrms.payroll.repository;

import kz.aitu.hrms.payroll.entity.PayrollAddition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollAdditionRepository extends JpaRepository<PayrollAddition, UUID> {

    Optional<PayrollAddition> findByIdAndDeletedFalse(UUID id);

    List<PayrollAddition> findByPeriodIdAndDeletedFalse(UUID periodId);

    List<PayrollAddition> findByPeriodIdAndEmployeeIdAndDeletedFalse(UUID periodId, UUID employeeId);

    List<PayrollAddition> findByEmployeeIdAndDeletedFalse(UUID employeeId);
}