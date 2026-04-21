package kz.aitu.hrms.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.employee.dto.EmployeeDtos;
import kz.aitu.hrms.employee.entity.EmploymentStatus;
import kz.aitu.hrms.employee.entity.EmploymentType;
import kz.aitu.hrms.employee.service.EmployeeImportExportService;
import kz.aitu.hrms.employee.service.EmployeeService;
import kz.aitu.hrms.employee.service.OrgChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Employees", description = "Employee lifecycle, onboarding/offboarding, org-chart")
@RestController
@RequestMapping("/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final OrgChartService orgChartService;
    private final EmployeeImportExportService importExportService;

    @Operation(summary = "Create a new employee (auto-generates employee_number)")
    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> create(
            @Valid @RequestBody EmployeeDtos.CreateEmployeeRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(employeeService.create(req)));
    }

    @Operation(summary = "List employees (scoped by role: ALL / TEAM / OWN)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<EmployeeDtos.EmployeeSummary>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) EmploymentType type,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.list(search, departmentId, status, type, pageable)));
    }

    @Operation(summary = "Get org chart (tree from top-level employees)")
    @GetMapping("/org-chart")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<EmployeeDtos.OrgChartNode>>> orgChart() {
        return ResponseEntity.ok(ApiResponse.ok(orgChartService.fullChart()));
    }

    @Operation(summary = "Get org-chart subtree rooted at the given employee")
    @GetMapping("/org-chart/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeDtos.OrgChartNode>> orgChartSubtree(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(orgChartService.subtree(id)));
    }

    @Operation(summary = "Bulk-import employees from XLSX")
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('EMPLOYEE_CREATE')")
    public ResponseEntity<ApiResponse<EmployeeDtos.ImportResult>> importXlsx(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(importExportService.importFromXlsx(file)));
    }

    @Operation(summary = "Export all employees as XLSX")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public ResponseEntity<InputStreamResource> exportXlsx() {
        InputStreamResource body = new InputStreamResource(importExportService.exportToXlsx());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }

    @Operation(summary = "Get employee by id")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.get(id)));
    }

    @Operation(summary = "Update an employee")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDtos.UpdateEmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.update(id, req)));
    }

    @Operation(summary = "Change employee employment status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDtos.UpdateStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.updateStatus(id, req)));
    }

    @Operation(summary = "Soft-delete an employee")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        employeeService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Employee deleted"));
    }

    @Operation(summary = "Publish EmployeeCreatedEvent → user-service creates an account")
    @PostMapping("/{id}/create-account")
    @PreAuthorize("hasAuthority('EMPLOYEE_UPDATE')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> createAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.createAccount(id)));
    }

    @Operation(summary = "Terminate employment")
    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('EMPLOYEE_DELETE')")
    public ResponseEntity<ApiResponse<EmployeeDtos.EmployeeResponse>> terminate(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeDtos.TerminateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(employeeService.terminate(id, req)));
    }
}