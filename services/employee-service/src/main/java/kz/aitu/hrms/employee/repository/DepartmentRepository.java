package kz.aitu.hrms.employee.repository;

import kz.aitu.hrms.employee.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByIdAndDeletedFalse(UUID id);

    List<Department> findAllByDeletedFalse();

    boolean existsByCodeAndDeletedFalse(String code);
}