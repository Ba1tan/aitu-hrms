package kz.aitu.hrms.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.attendance.dto.ScheduleDtos;
import kz.aitu.hrms.attendance.service.WorkScheduleService;
import kz.aitu.hrms.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Work Schedules", description = "Configurable shifts (default + per-department)")
@RestController
@RequestMapping("/v1/attendance/schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleService scheduleService;

    @Operation(summary = "List schedules (default first)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ScheduleDtos.ScheduleResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.list()));
    }

    @Operation(summary = "Create a schedule")
    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_MANAGE')")
    public ResponseEntity<ApiResponse<ScheduleDtos.ScheduleResponse>> create(
            @Valid @RequestBody ScheduleDtos.CreateScheduleRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(scheduleService.create(req)));
    }

    @Operation(summary = "Update a schedule")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_MANAGE')")
    public ResponseEntity<ApiResponse<ScheduleDtos.ScheduleResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleDtos.UpdateScheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.update(id, req)));
    }
}