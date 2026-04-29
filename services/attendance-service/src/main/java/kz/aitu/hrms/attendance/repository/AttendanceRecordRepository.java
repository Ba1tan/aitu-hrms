package kz.aitu.hrms.attendance.repository;

import kz.aitu.hrms.attendance.entity.AttendanceRecord;
import kz.aitu.hrms.attendance.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findByIdAndDeletedFalse(UUID id);

    Optional<AttendanceRecord> findByEmployeeIdAndWorkDateAndDeletedFalse(UUID employeeId, LocalDate workDate);

    boolean existsByEmployeeIdAndWorkDateAndDeletedFalse(UUID employeeId, LocalDate workDate);

    Optional<AttendanceRecord> findFirstByEmployeeIdAndDeletedFalseOrderByCreatedAtDesc(UUID employeeId);

    List<AttendanceRecord> findAllByWorkDateAndDeletedFalse(LocalDate workDate);

    List<AttendanceRecord> findAllByEmployeeIdAndWorkDateBetweenAndDeletedFalse(
            UUID employeeId, LocalDate from, LocalDate to);

    @Query("""
        SELECT r FROM AttendanceRecord r
        WHERE r.deleted = false
          AND (:employeeId IS NULL OR r.employeeId = :employeeId)
          AND (:from IS NULL OR r.workDate >= :from)
          AND (:to   IS NULL OR r.workDate <= :to)
        """)
    Page<AttendanceRecord> search(@Param("employeeId") UUID employeeId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  Pageable pageable);

    @Query("""
        SELECT r FROM AttendanceRecord r
        WHERE r.deleted = false
          AND r.employeeId IN :employeeIds
          AND r.workDate = :workDate
        """)
    List<AttendanceRecord> findByEmployeeIdsAndWorkDate(@Param("employeeIds") List<UUID> employeeIds,
                                                        @Param("workDate") LocalDate workDate);

    @Query("""
        SELECT COUNT(r) FROM AttendanceRecord r
        WHERE r.deleted = false
          AND r.employeeId = :employeeId
          AND r.workDate BETWEEN :from AND :to
          AND r.status = :status
        """)
    long countByEmployeeAndStatus(@Param("employeeId") UUID employeeId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("status") AttendanceStatus status);

    @Query("""
        SELECT COALESCE(SUM(r.workedMinutes), 0) FROM AttendanceRecord r
        WHERE r.deleted = false
          AND r.employeeId = :employeeId
          AND r.workDate BETWEEN :from AND :to
        """)
    long sumWorkedMinutes(@Param("employeeId") UUID employeeId,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(r.overtimeMinutes), 0) FROM AttendanceRecord r
        WHERE r.deleted = false
          AND r.employeeId = :employeeId
          AND r.workDate BETWEEN :from AND :to
        """)
    long sumOvertimeMinutes(@Param("employeeId") UUID employeeId,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to);

    @Query("""
        SELECT r.status, COUNT(r) FROM AttendanceRecord r
        WHERE r.deleted = false
          AND r.employeeId IN :employeeIds
          AND r.workDate BETWEEN :from AND :to
        GROUP BY r.status
        """)
    List<Object[]> aggregateStatusByEmployees(@Param("employeeIds") List<UUID> employeeIds,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to);

    @Query("""
        SELECT r.status, COUNT(r) FROM AttendanceRecord r
        WHERE r.deleted = false
          AND r.workDate BETWEEN :from AND :to
        GROUP BY r.status
        """)
    List<Object[]> aggregateStatusCompany(@Param("from") LocalDate from,
                                          @Param("to") LocalDate to);
}