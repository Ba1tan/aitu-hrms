package kz.aitu.hrms.payroll.repository;

import kz.aitu.hrms.payroll.entity.PayrollPeriod;
import kz.aitu.hrms.payroll.entity.PayrollPeriodStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, UUID> {

    Optional<PayrollPeriod> findByIdAndDeletedFalse(UUID id);

    Page<PayrollPeriod> findAllByDeletedFalseOrderByYearDescMonthDesc(Pageable pageable);

    boolean existsByYearAndMonthAndDeletedFalse(int year, int month);

    Optional<PayrollPeriod> findByYearAndMonthAndDeletedFalse(int year, int month);

    Optional<PayrollPeriod> findTopByStatusNotAndDeletedFalseOrderByYearDescMonthDesc(
            PayrollPeriodStatus status);
}