package kz.aitu.hrms.attendance.repository;

import kz.aitu.hrms.attendance.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {

    Optional<WorkSchedule> findByIdAndDeletedFalse(UUID id);

    Optional<WorkSchedule> findByIsDefaultTrueAndDeletedFalse();

    Optional<WorkSchedule> findByDepartmentIdAndDeletedFalse(UUID departmentId);

    List<WorkSchedule> findAllByDeletedFalseOrderByIsDefaultDescNameAsc();
}