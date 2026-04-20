package kz.aitu.hrms.employee.repository;

import kz.aitu.hrms.employee.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {

    Optional<Position> findByIdAndDeletedFalse(UUID id);

    List<Position> findAllByDeletedFalse();

    List<Position> findAllByDepartment_IdAndDeletedFalse(UUID departmentId);
}