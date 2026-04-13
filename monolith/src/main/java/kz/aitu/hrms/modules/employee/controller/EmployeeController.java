package kz.aitu.hrms.modules.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.response.ApiResponse;
import kz.aitu.hrms.modules.employee.dto.EmployeeDtos;
import kz.aitu.hrms.modules.employee.entity.EmploymentStatus;
import kz.aitu.hrms.modules.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Employees", description = "Employee management")
@RestController
@RequestMapping("/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @Operation(summary = "Create a new employee")
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> create(
            @Valid @RequestBody EmployeeDtos.CreateEmployeeRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(employeeService.create(request)));
    }

    @Operation(summary = "Get employee by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getById(id)));
    }

    @Operation(summary = "List employees with optional search and filters")
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER', 'ACCOUNTANT', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<EmployeeDtos.EmployeeSummary>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) EmploymentStatus status,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.getAll(search, departmentId, status, pageable)));
    }

    @Operation(summary = "Update employee details")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDtos.UpdateEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.update(id, request)));
    }

    @Operation(summary = "Update employee employment status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDtos.UpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.updateStatus(id, request)));
    }

    @Operation(summary = "Soft-delete an employee")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Employee deleted"));
    }
}
