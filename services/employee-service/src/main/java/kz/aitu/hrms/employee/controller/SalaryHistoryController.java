package kz.aitu.hrms.employee.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.employee.dto.SalaryHistoryDtos;
import kz.aitu.hrms.employee.service.SalaryHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Salary", description = "Salary history and changes")
@RestController
@RequestMapping("/v1/employees/{id}")
@RequiredArgsConstructor
public class SalaryHistoryController {

    private final SalaryHistoryService salaryHistoryService;

    @Operation(summary = "List salary history (most recent first)")
    @GetMapping("/salary-history")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DIRECTOR', 'HR_MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<SalaryHistoryDtos.SalaryHistoryResponse>>> list(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(salaryHistoryService.listForEmployee(id)));
    }

    @Operation(summary = "Record a salary change (updates base_salary and history)")
    @PostMapping("/salary-change")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<ApiResponse<SalaryHistoryDtos.SalaryHistoryResponse>> change(
            @PathVariable UUID id,
            @Valid @RequestBody SalaryHistoryDtos.SalaryChangeRequest req) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(salaryHistoryService.recordChange(id, req)));
    }
}