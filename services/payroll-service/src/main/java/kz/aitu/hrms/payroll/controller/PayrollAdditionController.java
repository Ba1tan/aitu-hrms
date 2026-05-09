package kz.aitu.hrms.payroll.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.payroll.dto.AdditionDtos;
import kz.aitu.hrms.payroll.service.PayrollAdditionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Payroll Additions", description = "Bonuses, allowances, fines and ad-hoc deductions per period")
@RestController
@RequestMapping("/v1/payroll/additions")
@RequiredArgsConstructor
public class PayrollAdditionController {

    private final PayrollAdditionService service;

    @Operation(summary = "List additions filtered by period and/or employee")
    @GetMapping
    @PreAuthorize("hasAuthority('PAYROLL_VIEW')")
    public ResponseEntity<ApiResponse<List<AdditionDtos.Response>>> list(
            @RequestParam(required = false) UUID periodId,
            @RequestParam(required = false) UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(service.list(periodId, employeeId)));
    }

    @Operation(summary = "Create an addition")
    @PostMapping
    @PreAuthorize("hasAuthority('PAYSLIP_ADJUST')")
    public ResponseEntity<ApiResponse<AdditionDtos.Response>> create(
            @Valid @RequestBody AdditionDtos.CreateRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(service.create(req)));
    }

    @Operation(summary = "Update an addition")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYSLIP_ADJUST')")
    public ResponseEntity<ApiResponse<AdditionDtos.Response>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdditionDtos.UpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

    @Operation(summary = "Delete an addition (soft delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYSLIP_ADJUST')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Addition deleted", null));
    }

    @Operation(summary = "Bulk-create the same addition for many employees in a period")
    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('PAYSLIP_ADJUST')")
    public ResponseEntity<ApiResponse<AdditionDtos.BulkResponse>> bulk(
            @Valid @RequestBody AdditionDtos.BulkRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(service.bulk(req)));
    }
}