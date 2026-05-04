package kz.aitu.hrms.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.leave.dto.LeaveTypeDtos;
import kz.aitu.hrms.leave.service.LeaveTypeService;
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

@Tag(name = "Leave Types", description = "Configure leave categories (Annual, Sick, etc.)")
@RestController
@RequestMapping("/v1/leave/types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService service;

    @Operation(summary = "List all leave types")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LeaveTypeDtos.Response>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.list()));
    }

    @Operation(summary = "Create a leave type")
    @PostMapping
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveTypeDtos.Response>> create(
            @Valid @RequestBody LeaveTypeDtos.UpsertRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(service.create(req)));
    }

    @Operation(summary = "Update a leave type")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<LeaveTypeDtos.Response>> update(
            @PathVariable UUID id,
            @Valid @RequestBody LeaveTypeDtos.UpsertRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

    @Operation(summary = "Soft-delete a leave type")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Leave type deleted", null));
    }
}