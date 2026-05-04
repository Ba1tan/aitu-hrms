package kz.aitu.hrms.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.leave.dto.LeaveBalanceDtos;
import kz.aitu.hrms.leave.security.CurrentUser;
import kz.aitu.hrms.leave.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Tag(name = "Leave Balances", description = "Annual entitlement, carryover, manual adjustments")
@RestController
@RequestMapping("/v1/leave/balances")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceService service;

    @Operation(summary = "My current-year leave balances")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDtos.Response>>> own() {
        return ResponseEntity.ok(ApiResponse.ok(service.own(CurrentUser.employeeId())));
    }

    @Operation(summary = "Balances for a specific employee (?year=)")
    @GetMapping("/employee/{id}")
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<List<LeaveBalanceDtos.Response>>> forEmployee(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(service.forEmployee(id, year)));
    }

    @Operation(summary = "Balance summary for a department (?year=)")
    @GetMapping("/department/{id}")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE_TEAM') or hasAuthority('LEAVE_APPROVE_ALL') or hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveBalanceDtos.DepartmentSummary>> forDepartment(
            @PathVariable UUID id,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(service.forDepartment(id, year)));
    }

    @Operation(summary = "Initialize balances for all active employees for the given year")
    @PostMapping("/initialize")
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveBalanceDtos.InitializeResponse>> initialize(
            @Valid @RequestBody LeaveBalanceDtos.InitializeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.initialize(req.getYear())));
    }

    @Operation(summary = "Apply a manual adjustment to a balance")
    @PutMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveBalanceDtos.Response>> adjust(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveBalanceDtos.AdjustRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.adjust(id, req)));
    }

    @Operation(summary = "Roll unused annual leave from a year into the next")
    @PostMapping("/carryover")
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveBalanceDtos.CarryoverResponse>> carryover(
            @Valid @RequestBody LeaveBalanceDtos.CarryoverRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.carryover(req.getFromYear())));
    }
}