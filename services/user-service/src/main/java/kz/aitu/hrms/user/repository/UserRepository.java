package kz.aitu.hrms.user.repository;

import kz.aitu.hrms.user.entity.Role;
import kz.aitu.hrms.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmployeeId(UUID employeeId);

    boolean existsByEmail(String email);

    @Query("""
           SELECT u FROM User u
           WHERE u.deleted = false
             AND (:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
                                 OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                                 OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:role   IS NULL OR u.role = :role)
           """)
    Page<User> search(@Param("search") String search,
                      @Param("role") Role role,
                      Pageable pageable);
}