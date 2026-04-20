package kz.aitu.hrms.employee.repository;

import kz.aitu.hrms.employee.entity.Employee;
import kz.aitu.hrms.employee.entity.EmploymentStatus;
import kz.aitu.hrms.employee.entity.EmploymentType;
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

    Optional<Employee> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByIinAndDeletedFalse(String iin);

    long countByDeletedFalse();

    List<Employee> findAllByDeletedFalse();

    List<Employee> findAllByManager_IdAndDeletedFalse(UUID managerId);

    @Query("""
        SELECT e FROM Employee e
        WHERE e.deleted = false
          AND (:search IS NULL OR :search = ''
               OR LOWER(e.firstName)      LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.lastName)       LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.middleName)     LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.email)          LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:departmentId IS NULL OR e.department.id = :departmentId)
          AND (:status IS NULL OR e.status = :status)
          AND (:type   IS NULL OR e.employmentType = :type)
          AND (:scopeManagerId IS NULL OR e.manager.id = :scopeManagerId)
          AND (:scopeSelfId   IS NULL OR e.id = :scopeSelfId)
        """)
    Page<Employee> search(@Param("search") String search,
                          @Param("departmentId") UUID departmentId,
                          @Param("status") EmploymentStatus status,
                          @Param("type") EmploymentType type,
                          @Param("scopeManagerId") UUID scopeManagerId,
                          @Param("scopeSelfId") UUID scopeSelfId,
                          Pageable pageable);
}