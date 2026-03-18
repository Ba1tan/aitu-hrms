package kz.aitu.hrms.modules.payroll.repository;

import kz.aitu.hrms.modules.payroll.entity.Payslip;
import kz.aitu.hrms.modules.payroll.enums.PayslipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, UUID> {

    Optional<Payslip> findByIdAndDeletedFalse(UUID id);

    // All payslips for a period (paginated) — HR view
    Page<Payslip> findByPeriodIdAndDeletedFalse(UUID periodId, Pageable pageable);

    // All payslips for a period (list) — used by payroll processing and reports
    List<Payslip> findByPeriodIdAndDeletedFalse(UUID periodId);

    // Employee self-service: their own payslips across all periods
    Page<Payslip> findByEmployeeIdAndDeletedFalseOrderByPeriodYearDescPeriodMonthDesc(
            UUID employeeId, Pageable pageable);

    // Check if payslip already exists for this employee+period (prevent duplicates)
    boolean existsByPeriodIdAndEmployeeIdAndDeletedFalse(UUID periodId, UUID employeeId);

    // Totals for a period — used in period summary response
    @Query("""
        SELECT COALESCE(SUM(p.grossSalary), 0)  AS totalGross,
               COALESCE(SUM(p.netSalary), 0)    AS totalNet,
               COALESCE(SUM(p.ipnAmount), 0)    AS totalIpn,
               COALESCE(SUM(p.opvAmount), 0)    AS totalOpv,
               COALESCE(SUM(p.soAmount), 0)     AS totalSo,
               COUNT(p)                          AS count
        FROM Payslip p
        WHERE p.period.id = :periodId AND p.deleted = false
        """)
    List<Object[]> getPeriodTotals(@Param("periodId") UUID periodId);

    // Employee's single payslip for a specific period
    Optional<Payslip> findByPeriodIdAndEmployeeIdAndDeletedFalse(UUID periodId, UUID employeeId);

    // Count payslips by status for a period
    long countByPeriodIdAndStatusAndDeletedFalse(UUID periodId, PayslipStatus status);

    // Sum net salary for period — for bank transfer file
    @Query("SELECT COALESCE(SUM(p.netSalary), 0) FROM Payslip p WHERE p.period.id = :periodId AND p.deleted = false")
    BigDecimal sumNetSalaryByPeriod(@Param("periodId") UUID periodId);
}
