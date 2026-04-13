package kz.aitu.hrms.modules.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.response.ApiResponse;
import kz.aitu.hrms.modules.employee.dto.DepartmentDtos;
import kz.aitu.hrms.modules.employee.dto.PositionDtos;
import kz.aitu.hrms.modules.employee.service.DepartmentService;
import kz.aitu.hrms.modules.employee.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Departments & Positions")
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final PositionService positionService;

    @Operation(summary = "Create department")
    @PostMapping("/departments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<DepartmentDtos.DepartmentResponse>> createDept(
            @Valid @RequestBody DepartmentDtos.CreateDepartmentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(departmentService.create(request)));
    }

    @Operation(summary = "List all departments")
    @GetMapping("/departments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DepartmentDtos.DepartmentResponse>>> listDepts() {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.getAll()));
    }

    @Operation(summary = "Get department by ID")
    @GetMapping("/departments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DepartmentDtos.DepartmentResponse>> getDept(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.getById(id)));
    }

    @Operation(summary = "Update department")
    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<DepartmentDtos.DepartmentResponse>> updateDept(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentDtos.UpdateDepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(departmentService.update(id, request)));
    }

    @Operation(summary = "Delete department")
    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDept(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Department deleted"));
    }

    @Operation(summary = "Create position")
    @PostMapping("/positions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<PositionDtos.PositionResponse>> createPos(
            @Valid @RequestBody PositionDtos.CreatePositionRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(positionService.create(request)));
    }

    @Operation(summary = "List all positions")
    @GetMapping("/positions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PositionDtos.PositionResponse>>> listPositions(
            @RequestParam(required = false) UUID departmentId) {
        if (departmentId != null) {
            return ResponseEntity.ok(ApiResponse.ok(positionService.getByDepartment(departmentId)));
        }
        return ResponseEntity.ok(ApiResponse.ok(positionService.getAll()));
    }

    @Operation(summary = "Get position by ID")
    @GetMapping("/positions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PositionDtos.PositionResponse>> getPos(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(positionService.getById(id)));
    }

    @Operation(summary = "Update position")
    @PutMapping("/positions/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<PositionDtos.PositionResponse>> updatePos(
            @PathVariable UUID id,
            @Valid @RequestBody PositionDtos.UpdatePositionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(positionService.update(id, request)));
    }

    @Operation(summary = "Delete position")
    @DeleteMapping("/positions/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePos(@PathVariable UUID id) {
        positionService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Position deleted"));
    }
}
