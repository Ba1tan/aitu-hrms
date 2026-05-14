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

    /**
     * Used by the one-time bootstrap flow to detect whether the tenant
     * still needs its first admin. We count both deleted and non-deleted
     * rows so a soft-deleted admin can't be used as a backdoor to re-open
     * registration.
     */
    long countByRole(Role role);

    /**
     * The CAST(:search AS string) is load-bearing — Postgres can't infer the
     * type of an untyped null inside CONCAT('%', ?, '%') and falls back to
     * bytea, which has no LOWER overload (SQLState 42883). Casting forces
     * Hibernate to emit `cast(? as varchar)`, so the bind is text even when
     * the value is null.
     */
    @Query("""
           SELECT u FROM User u
           WHERE u.deleted = false
             AND (:search IS NULL OR LOWER(u.email)     LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                                 OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                                 OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
             AND (:role   IS NULL OR u.role = :role)
           """)
    Page<User> search(@Param("search") String search,
                      @Param("role") Role role,
                      Pageable pageable);
}