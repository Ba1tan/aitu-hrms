package kz.aitu.hrms.user.repository;

import kz.aitu.hrms.user.entity.Role;
import kz.aitu.hrms.user.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermission.Pk> {

    @Query("""
           SELECT p.code FROM Permission p
           JOIN RolePermission rp ON rp.permissionId = p.id
           WHERE rp.role = :role
           """)
    Set<String> findPermissionCodesByRole(@Param("role") Role role);
}