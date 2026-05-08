package kz.aitu.hrms.leave.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.leave.dto.CalendarDtos;
import kz.aitu.hrms.leave.service.LeaveCalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Leave Calendar", description = "Approved leaves grouped by month/department")
@RestController
@RequestMapping("/v1/leave/calendar")
@RequiredArgsConstructor
public class LeaveCalendarController {

    private final LeaveCalendarService service;

    @Operation(summary = "Approved leaves for a month (defaults to current month)")
    @GetMapping
    @PreAuthorize("hasAuthority('LEAVE_APPROVE_TEAM') or hasAuthority('LEAVE_APPROVE_ALL') or hasAuthority('LEAVE_BALANCE_MANAGE')")
    public ResponseEntity<ApiResponse<List<CalendarDtos.Entry>>> calendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) UUID departmentId) {
        return ResponseEntity.ok(ApiResponse.ok(service.calendar(year, month, departmentId)));
    }
}