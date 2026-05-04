package kz.aitu.hrms.leave.repository;

import kz.aitu.hrms.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    Optional<LeaveBalance> findByIdAndDeletedFalse(UUID id);

    @Query("""
        SELECT b FROM LeaveBalance b
        WHERE b.deleted = false
          AND b.employeeId = :employeeId
          AND b.leaveType.id = :leaveTypeId
          AND b.year = :year
        """)
    Optional<LeaveBalance> findOne(@Param("employeeId") UUID employeeId,
                                   @Param("leaveTypeId") UUID leaveTypeId,
                                   @Param("year") int year);

    @Query("""
        SELECT b FROM LeaveBalance b
        WHERE b.deleted = false
          AND b.employeeId = :employeeId
          AND b.year = :year
        """)
    List<LeaveBalance> findForEmployeeYear(@Param("employeeId") UUID employeeId,
                                           @Param("year") int year);

    @Query("""
        SELECT b FROM LeaveBalance b
        WHERE b.deleted = false
          AND b.employeeId IN :employeeIds
          AND b.year = :year
        """)
    List<LeaveBalance> findForEmployeesYear(@Param("employeeIds") List<UUID> employeeIds,
                                            @Param("year") int year);

    @Query("""
        SELECT b FROM LeaveBalance b
        WHERE b.deleted = false
          AND b.year = :year
          AND b.leaveType.id IN :leaveTypeIds
        """)
    List<LeaveBalance> findByYearAndTypes(@Param("year") int year,
                                          @Param("leaveTypeIds") List<UUID> leaveTypeIds);
}