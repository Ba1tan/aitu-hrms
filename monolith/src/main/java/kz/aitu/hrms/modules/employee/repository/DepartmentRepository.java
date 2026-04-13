package kz.aitu.hrms.modules.employee.repository;

import kz.aitu.hrms.modules.employee.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByIdAndDeletedFalse(UUID id);

    List<Department> findAllByDeletedFalseOrderByName();

    boolean existsByNameAndDeletedFalse(String name);

    @Query("SELECT d FROM Department d WHERE d.deleted = false AND d.parent IS NULL ORDER BY d.name")
    List<Department> findRootDepartments();
}
