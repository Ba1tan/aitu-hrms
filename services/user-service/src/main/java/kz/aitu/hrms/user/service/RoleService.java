package kz.aitu.hrms.user.service;

import kz.aitu.hrms.common.exception.BusinessException;
import kz.aitu.hrms.user.dto.AdminDtos;
import kz.aitu.hrms.user.entity.Permission;
import kz.aitu.hrms.user.entity.Role;
import kz.aitu.hrms.user.entity.RolePermission;
import kz.aitu.hrms.user.entity.User;
import kz.aitu.hrms.user.exception.ConflictException;
import kz.aitu.hrms.user.repository.PermissionRepository;
import kz.aitu.hrms.user.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Backs the role↔permission matrix endpoints for the Phase 1B admin UI.
 *
 * <p>Permission changes are <b>not</b> propagated to existing JWTs — tokens
 * carry the authorities they were minted with. The new set only lands in
 * tokens issued after the change (login / refresh), since
 * {@code AuthService} reads {@code role_permissions} fresh each time. See
 * {@code docs/PERMISSIONS.md §6}. There is therefore no per-user permission
 * cache to invalidate here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AdminDtos.RolePermissionMatrixResponse getMatrix() {
        List<Permission> permissions = permissionRepository.findAllByOrderByModuleAscCodeAsc();

        List<AdminDtos.PermissionDto> permissionDtos = permissions.stream()
                .map(p -> AdminDtos.PermissionDto.builder()
                        .code(p.getCode())
                        .module(p.getModule())
                        .description(p.getDescription())
                        .build())
                .toList();

        // Seed every role with an empty list so roles with no grants still appear.
        Map<String, List<String>> matrix = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            matrix.put(role.name(), new ArrayList<>());
        }
        for (RolePermissionRepository.RoleCodePair pair : rolePermissionRepository.findAllRoleCodePairs()) {
            matrix.get(pair.getRole().name()).add(pair.getCode());
        }
        matrix.values().forEach(codes -> codes.sort(String::compareTo));

        List<String> roles = java.util.Arrays.stream(Role.values()).map(Enum::name).toList();
        return AdminDtos.RolePermissionMatrixResponse.builder()
                .roles(roles)
                .permissions(permissionDtos)
                .matrix(matrix)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.PermissionDto> getPermissionCatalog() {
        return getMatrix().getPermissions();
    }

    @Transactional
    public void updateRolePermissions(String roleName, List<String> add, List<String> remove) {
        Role role = parseRole(roleName);
        if (role == Role.SUPER_ADMIN) {
            throw new ConflictException(
                    "SUPER_ADMIN permissions are managed automatically and cannot be edited");
        }

        List<String> toAdd = add == null ? List.of() : add;
        List<String> toRemove = remove == null ? List.of() : remove;
        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            return;
        }

        // Resolve codes → ids up front so an unknown code fails the whole request.
        Map<String, Permission> byCode = permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getCode, p -> p));

        TreeSet<String> before = new TreeSet<>(rolePermissionRepository.findPermissionCodesByRole(role));

        for (String code : toAdd) {
            Permission permission = resolve(byCode, code);
            RolePermission.Pk pk = new RolePermission.Pk(role, permission.getId());
            if (!rolePermissionRepository.existsById(pk)) {
                rolePermissionRepository.save(RolePermission.builder()
                        .role(role)
                        .permissionId(permission.getId())
                        .build());
            }
        }
        for (String code : toRemove) {
            Permission permission = resolve(byCode, code);
            UUID permId = permission.getId();
            rolePermissionRepository.deleteById(new RolePermission.Pk(role, permId));
        }

        TreeSet<String> after = new TreeSet<>(rolePermissionRepository.findPermissionCodesByRole(role));

        auditService.log(currentUserIdOrNull(), currentUserEmailOrNull(),
                "UPDATE", "ROLE_PERMISSION", null,
                Map.of("role", role.name(), "permissions", before),
                Map.of("role", role.name(), "permissions", after));

        log.info("Role {} permissions updated by {}: +{} -{}",
                role, currentUserEmailOrNull(), toAdd, toRemove);
    }

    // ── internals ──

    private Permission resolve(Map<String, Permission> byCode, String code) {
        Permission permission = byCode.get(code);
        if (permission == null) {
            throw new BusinessException("Unknown permission code: " + code);
        }
        return permission;
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid role: " + role);
        }
    }

    private UUID currentUserIdOrNull() {
        Object principal = currentPrincipal();
        return (principal instanceof User u) ? u.getId() : null;
    }

    private String currentUserEmailOrNull() {
        Object principal = currentPrincipal();
        return (principal instanceof User u) ? u.getEmail() : null;
    }

    private Object currentPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}