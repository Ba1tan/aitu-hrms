package kz.aitu.hrms.user.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Payloads for the Phase 1B admin UI endpoints — audit log + role/permission
 * matrix. Shapes mirror {@code frontend/hrms-web/shared/api.ts}
 * ({@code AuditLogEntry}, {@code RolePermissionMatrix}).
 */
public class AdminDtos {

    private AdminDtos() {}

    /** One row of {@code GET /v1/users/audit} (frontend {@code AuditLogEntry}). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditLogResponse {
        private UUID id;
        private LocalDateTime timestamp;
        private UUID actorId;
        private String actorEmail;
        private String action;
        private String entityType;
        private UUID entityId;
        private String ipAddress;
        /** Parsed back from the stored JSONB so it serializes as an object, not a string. */
        private JsonNode oldValue;
        private JsonNode newValue;
    }

    /** A single permission column header (frontend {@code RolePermissionMatrix.permissions[]}). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionDto {
        private String code;
        private String module;
        private String description;
    }

    /** Response of {@code GET /v1/users/roles} (frontend {@code RolePermissionMatrix}). */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RolePermissionMatrixResponse {
        private List<String> roles;
        private List<PermissionDto> permissions;
        /** role → granted permission codes. */
        private Map<String, List<String>> matrix;
    }

    /** Request body of {@code POST /v1/users/roles/{role}/permissions}. */
    @Data
    public static class UpdateRolePermissionsRequest {
        @NotNull
        private List<String> add;
        @NotNull
        private List<String> remove;
    }
}
