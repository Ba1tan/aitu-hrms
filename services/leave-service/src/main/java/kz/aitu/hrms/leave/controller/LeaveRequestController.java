package kz.aitu.hrms.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.leave.dto.LeaveRequestDtos;
import kz.aitu.hrms.leave.entity.LeaveStatus;
import kz.aitu.hrms.leave.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.UUID;

@Tag(name = "Leave Requests", description = "Submit, review, and manage leave requests")
@RestController
@RequestMapping("/v1/leave/requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService service;

    @Operation(summary = "Submit a leave request")
    @PostMapping
    @PreAuthorize("hasAuthority('LEAVE_REQUEST_OWN')")
    public ResponseEntity<ApiResponse<LeaveRequestDtos.Response>> create(
            @Valid @RequestBody LeaveRequestDtos.CreateRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(service.create(req)));
    }

    @Operation(summary = "My leave requests (paginated)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<LeaveRequestDtos.Response>>> own(
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.own(status, pageable)));
    }

    @Operation(summary = "Pending requests awaiting my approval")
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE_TEAM') or hasAuthority('LEAVE_APPROVE_ALL')")
    public ResponseEntity<ApiResponse<Page<LeaveRequestDtos.Response>>> pending(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.pendingForApprover(pageable)));
    }

    @Operation(summary = "My team's leave requests")
    @GetMapping("/team")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE_TEAM') or hasAuthority('LEAVE_APPROVE_ALL')")
    public ResponseEntity<ApiResponse<Page<LeaveRequestDtos.Response>>> team(
            @RequestParam(required = false) LeaveStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.team(status, pageable)));
    }

    @Operation(summary = "All leave requests (HR view)")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE_ALL')")
    public ResponseEntity<ApiResponse<Page<LeaveRequestDtos.Response>>> all(
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) UUID departmentId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(service.all(status, departmentId, pageable)));
    }

    @Operation(summary = "Leave request detail")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LeaveRequestDtos.Response>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.detail(id)));
    }

    @Operation(summary = "Approve a leave request")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE_TEAM') or hasAuthority('LEAVE_APPROVE_ALL')")
    public ResponseEntity<ApiResponse<LeaveRequestDtos.Response>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) LeaveRequestDtos.ReviewRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.approve(id, req)));
    }

    @Operation(summary = "Reject a leave request")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LEAVE_APPROVE_TEAM') or hasAuthority('LEAVE_APPROVE_ALL')")
    public ResponseEntity<ApiResponse<LeaveRequestDtos.Response>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) LeaveRequestDtos.ReviewRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.reject(id, req)));
    }

    @Operation(summary = "Cancel a leave request")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LeaveRequestDtos.Response>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.cancel(id)));
    }
}