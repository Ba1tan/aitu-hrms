package kz.aitu.hrms.payroll.repository;

import kz.aitu.hrms.payroll.entity.Payslip;
import kz.aitu.hrms.payroll.entity.PayslipStatus;
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

    Page<Payslip> findByPeriodIdAndDeletedFalse(UUID periodId, Pageable pageable);

    List<Payslip> findByPeriodIdAndDeletedFalse(UUID periodId);

    /**
     * The CAST(:search AS string) is load-bearing — Postgres can't infer the
     * type of an untyped null inside CONCAT('%', ?, '%') and falls back to
     * bytea, which has no LOWER overload (SQLState 42883). Casting forces
     * Hibernate to emit `cast(? as varchar)` in the SQL, so the bind is text
     * even when the value is null. H2's PostgreSQL mode is more forgiving,
     * which is why the test suite never caught this.
     */
    @Query("""
        SELECT p FROM Payslip p
        WHERE p.period.id = :periodId AND p.deleted = false
          AND (:status IS NULL OR p.status = :status)
          AND (:search IS NULL OR LOWER(p.employeeName)   LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                              OR LOWER(p.employeeNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """)
    Page<Payslip> search(@Param("periodId") UUID periodId,
                         @Param("status") PayslipStatus status,
                         @Param("search") String search,
                         Pageable pageable);

    @Query("""
        SELECT p FROM Payslip p
        WHERE p.employeeId = :employeeId AND p.deleted = false
        ORDER BY p.period.year DESC, p.period.month DESC
        """)
    Page<Payslip> findMyPayslips(@Param("employeeId") UUID employeeId, Pageable pageable);

    boolean existsByPeriodIdAndEmployeeIdAndDeletedFalse(UUID periodId, UUID employeeId);

    Optional<Payslip> findByPeriodIdAndEmployeeIdAndDeletedFalse(UUID periodId, UUID employeeId);

    long countByPeriodIdAndStatusAndDeletedFalse(UUID periodId, PayslipStatus status);

    long countByPeriodIdAndDeletedFalse(UUID periodId);

    @Query("""
        SELECT COALESCE(SUM(p.grossSalary),  0),
               COALESCE(SUM(p.netSalary),    0),
               COALESCE(SUM(p.ipnAmount),    0),
               COALESCE(SUM(p.opvAmount),    0),
               COALESCE(SUM(p.vosmsAmount),  0),
               COALESCE(SUM(p.soAmount),     0),
               COALESCE(SUM(p.snAmount),     0),
               COALESCE(SUM(p.opvrAmount),   0),
               COUNT(p)
        FROM Payslip p
        WHERE p.period.id = :periodId AND p.deleted = false
        """)
    List<Object[]> getPeriodTotals(@Param("periodId") UUID periodId);

    @Query("""
        SELECT COALESCE(SUM(p.netSalary), 0) FROM Payslip p
        WHERE p.period.id = :periodId AND p.deleted = false
        """)
    BigDecimal sumNetSalaryByPeriod(@Param("periodId") UUID periodId);

    /** Cumulative tax totals for an employee across a calendar year (YTD). */
    @Query("""
        SELECT COALESCE(SUM(p.grossSalary),  0),
               COALESCE(SUM(p.earnedSalary), 0),
               COALESCE(SUM(p.netSalary),    0),
               COALESCE(SUM(p.opvAmount),    0),
               COALESCE(SUM(p.vosmsAmount),  0),
               COALESCE(SUM(p.ipnAmount),    0),
               COALESCE(SUM(p.soAmount),     0),
               COALESCE(SUM(p.snAmount),     0),
               COALESCE(SUM(p.opvrAmount),   0),
               COUNT(p)
        FROM Payslip p
        WHERE p.employeeId = :employeeId
          AND p.period.year = :year
          AND p.deleted = false
        """)
    List<Object[]> getYearToDate(@Param("employeeId") UUID employeeId,
                                 @Param("year") int year);
}