package kz.aitu.hrms.modules.employee.repository;

import kz.aitu.hrms.modules.employee.entity.Employee;
import kz.aitu.hrms.modules.employee.entity.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByIdAndDeletedFalse(UUID id);

    @Query("""
        SELECT e FROM Employee e
        WHERE e.deleted = false
          AND (:search IS NULL OR :search = ''
               OR LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.email)     LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:departmentId IS NULL OR e.department.id = :departmentId)
          AND (:status IS NULL OR e.status = :status)
        """)
    Page<Employee> searchEmployees(
            @Param("search") String search,
            @Param("departmentId") UUID departmentId,
            @Param("status") EmploymentStatus status,
            Pageable pageable);

    List<Employee> findByStatusAndDeletedFalse(EmploymentStatus status);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.deleted = false")
    long countActive();

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByIinAndDeletedFalse(String iin);

    Optional<Employee> findByEmailAndDeletedFalse(String email);

    @Query("SELECT MAX(e.employeeNumber) FROM Employee e")
    Optional<String> findMaxEmployeeNumber();
}
