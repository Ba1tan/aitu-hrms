package kz.aitu.hrms.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.attendance.dto.HolidayDtos;
import kz.aitu.hrms.attendance.service.HolidayService;
import kz.aitu.hrms.common.dto.ApiResponse;
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

@Tag(name = "Holidays", description = "Kazakhstan public holidays calendar")
@RestController
@RequestMapping("/v1/attendance/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "List holidays (optionally filtered by year)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<HolidayDtos.HolidayResponse>>> list(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.ok(holidayService.list(year)));
    }

    @Operation(summary = "Create a holiday")
    @PostMapping
    @PreAuthorize("hasAuthority('ATTENDANCE_MANAGE')")
    public ResponseEntity<ApiResponse<HolidayDtos.HolidayResponse>> create(
            @Valid @RequestBody HolidayDtos.CreateHolidayRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(holidayService.create(req)));
    }

    @Operation(summary = "Update a holiday")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_MANAGE')")
    public ResponseEntity<ApiResponse<HolidayDtos.HolidayResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody HolidayDtos.UpdateHolidayRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(holidayService.update(id, req)));
    }

    @Operation(summary = "Delete a holiday")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        holidayService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Holiday deleted"));
    }
}