package kz.aitu.hrms.modules.employee.repository;

import kz.aitu.hrms.modules.employee.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {

    Optional<Position> findByIdAndDeletedFalse(UUID id);

    List<Position> findAllByDeletedFalseOrderByTitle();

    List<Position> findByDepartmentIdAndDeletedFalse(UUID departmentId);
}
