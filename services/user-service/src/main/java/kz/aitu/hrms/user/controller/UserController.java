package kz.aitu.hrms.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.user.dto.UserDtos;
import kz.aitu.hrms.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Users", description = "User management (SYSTEM_USERS permission)")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYSTEM_USERS') or hasRole('SUPER_ADMIN')")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List all users (paginated, ?search= & ?role=)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDtos.UserSummary>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.list(search, role, pageable)));
    }

    @Operation(summary = "Get user by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDtos.UserSummary>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
    }

    @Operation(summary = "Create a new user")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDtos.UserSummary>> create(
            @Valid @RequestBody UserDtos.CreateUserRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(userService.create(request)));
    }

    @Operation(summary = "Update user (role, name, enabled, locked)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDtos.UserSummary>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserDtos.UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request)));
    }

    @Operation(summary = "Soft-delete user")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.noContent("User deleted"));
    }

    @Operation(summary = "Link user to employee record")
    @PutMapping("/{id}/link-employee")
    public ResponseEntity<ApiResponse<UserDtos.UserSummary>> linkEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UserDtos.LinkEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.linkEmployee(id, request.getEmployeeId())));
    }
}