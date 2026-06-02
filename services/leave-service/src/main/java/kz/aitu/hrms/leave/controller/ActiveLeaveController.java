package kz.aitu.hrms.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.leave.dto.ActiveLeaveDtos;
import kz.aitu.hrms.leave.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * "Who is on approved leave right now" — read-only views consumed by
 * employee-service (to derive ON_LEAVE badges) and reporting-service (for
 * dashboard counts). Authenticated-only; the data is the same set already
 * surfaced by the team calendar.
 */
@Tag(name = "Active Leave", description = "Approved leaves intersecting today")
@RestController
@RequestMapping("/v1/leave/active")
@RequiredArgsConstructor
public class ActiveLeaveController {

    private final LeaveRequestService service;

    @Operation(summary = "Employees currently on approved leave (optional employeeIds filter)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ActiveLeaveDtos.ActiveLeaveDto>>> activeToday(
            @RequestParam(required = false) List<UUID> employeeIds) {
        return ResponseEntity.ok(ApiResponse.ok(service.activeToday(employeeIds)));
    }

    @Operation(summary = "Count of distinct employees on approved leave today")
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ActiveLeaveDtos.ActiveLeaveCountDto>> count() {
        long count = service.activeTodayCount();
        return ResponseEntity.ok(ApiResponse.ok(
                ActiveLeaveDtos.ActiveLeaveCountDto.builder().count(count).build()));
    }
}