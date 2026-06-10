package kz.aitu.hrms.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.user.dto.AdminDtos;
import kz.aitu.hrms.user.service.AuditQueryService;
import kz.aitu.hrms.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Phase 1B admin endpoints: audit log + role/permission matrix.
 *
 * <p>Lives alongside {@link UserController} on {@code /v1/users} but is gated
 * by the finer-grained {@code SYSTEM_AUDIT} / {@code SYSTEM_ROLES} permissions
 * rather than {@code SYSTEM_USERS} — DIRECTOR, for instance, can read the
 * audit log without being able to manage users. Literal paths
 * ({@code /audit}, {@code /roles}, {@code /permissions}) take precedence over
 * {@code UserController}'s {@code /{id}} pattern.
 */
@Tag(name = "Admin", description = "Audit log + role/permission matrix")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class AdminController {

    private final AuditQueryService auditQueryService;
    private final RoleService roleService;

    @Operation(summary = "Paginated audit log (filter by actor, entityType, action, date range)")
    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('SYSTEM_AUDIT')")
    public ResponseEntity<ApiResponse<Page<AdminDtos.AuditLogResponse>>> audit(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                auditQueryService.search(actor, entityType, action, from, to, pageable)));
    }

    @Operation(summary = "Role ↔ permission matrix")
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('SYSTEM_ROLES')")
    public ResponseEntity<ApiResponse<AdminDtos.RolePermissionMatrixResponse>> rolesMatrix() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getMatrix()));
    }

    @Operation(summary = "Flat permission catalog")
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('SYSTEM_ROLES')")
    public ResponseEntity<ApiResponse<List<AdminDtos.PermissionDto>>> permissions() {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getPermissionCatalog()));
    }

    @Operation(summary = "Add/remove permissions for a role (SUPER_ADMIN is read-only)")
    @PostMapping("/roles/{role}/permissions")
    @PreAuthorize("hasAuthority('SYSTEM_ROLES')")
    public ResponseEntity<ApiResponse<Void>> updateRolePermissions(
            @PathVariable String role,
            @Valid @RequestBody AdminDtos.UpdateRolePermissionsRequest request) {
        roleService.updateRolePermissions(role, request.getAdd(), request.getRemove());
        return ResponseEntity.ok(ApiResponse.noContent(
                "Role updated. Changes apply to tokens issued after this point — "
                        + "active sessions keep their permissions until refresh."));
    }
}