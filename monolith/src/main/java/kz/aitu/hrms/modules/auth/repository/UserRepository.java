package kz.aitu.hrms.modules.auth.repository;

import kz.aitu.hrms.modules.auth.entity.Role;
import kz.aitu.hrms.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);
}
