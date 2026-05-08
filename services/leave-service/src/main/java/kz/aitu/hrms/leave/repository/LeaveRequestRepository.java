package kz.aitu.hrms.leave.repository;

import kz.aitu.hrms.leave.entity.LeaveRequest;
import kz.aitu.hrms.leave.entity.LeaveStatus;
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
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    Optional<LeaveRequest> findByIdAndDeletedFalse(UUID id);

    @Query("""
        SELECT r FROM LeaveRequest r
        WHERE r.deleted = false
          AND r.employeeId = :employeeId
          AND (:status IS NULL OR r.status = :status)
        """)
    Page<LeaveRequest> findOwn(@Param("employeeId") UUID employeeId,
                               @Param("status") LeaveStatus status,
                               Pageable pageable);

    @Query("""
        SELECT r FROM LeaveRequest r
        WHERE r.deleted = false
          AND r.employeeId IN :employeeIds
          AND (:status IS NULL OR r.status = :status)
        """)
    Page<LeaveRequest> findByEmployees(@Param("employeeIds") List<UUID> employeeIds,
                                       @Param("status") LeaveStatus status,
                                       Pageable pageable);

    @Query("""
        SELECT r FROM LeaveRequest r
        WHERE r.deleted = false
          AND r.employeeId IN :employeeIds
          AND r.status = :status
        """)
    Page<LeaveRequest> findPendingForApprovers(@Param("employeeIds") List<UUID> employeeIds,
                                               @Param("status") LeaveStatus status,
                                               Pageable pageable);

    @Query("""
        SELECT r FROM LeaveRequest r
        WHERE r.deleted = false
          AND (:status IS NULL OR r.status = :status)
        """)
    Page<LeaveRequest> findAll(@Param("status") LeaveStatus status, Pageable pageable);

    /**
     * Approved requests that overlap [start, end] for a specific employee.
     * Used by the overlap business rule on submit. Includes PENDING too — once
     * pending, no second request can target the same window.
     */
    @Query("""
        SELECT r FROM LeaveRequest r
        WHERE r.deleted = false
          AND r.employeeId = :employeeId
          AND r.status IN (kz.aitu.hrms.leave.entity.LeaveStatus.PENDING,
                           kz.aitu.hrms.leave.entity.LeaveStatus.APPROVED)
          AND r.startDate <= :end
          AND r.endDate   >= :start
        """)
    List<LeaveRequest> findOverlapping(@Param("employeeId") UUID employeeId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    /**
     * APPROVED requests intersecting a calendar window. Used by /calendar.
     */
    @Query("""
        SELECT r FROM LeaveRequest r
        WHERE r.deleted = false
          AND r.status = kz.aitu.hrms.leave.entity.LeaveStatus.APPROVED
          AND (:employeeIds IS NULL OR r.employeeId IN :employeeIds)
          AND r.startDate <= :to
          AND r.endDate   >= :from
        """)
    List<LeaveRequest> findApprovedInRange(@Param("employeeIds") List<UUID> employeeIds,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);
}