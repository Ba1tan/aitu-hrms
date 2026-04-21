package kz.aitu.hrms.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.employee.dto.DepartmentDtos;
import kz.aitu.hrms.employee.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Departments", description = "Department CRUD")
@RestController
@RequestMapping("/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Operation(summary = "Create a new department")
    @PostMapping
    @PreAuthorize("hasAuthority('DEPT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentDtos.DepartmentResponse>> create(
            @Valid @RequestBody DepartmentDtos.CreateDepartmentRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(departmentService.create(req)));
    }

    @Operation(summary = "List departments (with employee count)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DepartmentDtos.DepartmentResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.list()));
    }

    @Operation(summary = "Get department by id")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DepartmentDtos.DepartmentResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.get(id)));
    }

    @Operation(summary = "Update department")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPT_MANAGE')")
    public ResponseEntity<ApiResponse<DepartmentDtos.DepartmentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentDtos.UpdateDepartmentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.update(id, req)));
    }

    @Operation(summary = "Delete department (only if empty)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPT_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Department deleted"));
    }
}