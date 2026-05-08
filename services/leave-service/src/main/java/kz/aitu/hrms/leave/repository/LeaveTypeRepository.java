package kz.aitu.hrms.leave.repository;

import kz.aitu.hrms.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    List<LeaveType> findAllByDeletedFalseOrderByName();

    Optional<LeaveType> findByIdAndDeletedFalse(UUID id);

    Optional<LeaveType> findByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalse(String code);
}