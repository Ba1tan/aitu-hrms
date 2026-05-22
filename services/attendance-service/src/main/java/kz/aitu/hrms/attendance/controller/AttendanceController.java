package kz.aitu.hrms.attendance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.aitu.hrms.attendance.dto.AttendanceDtos;
import kz.aitu.hrms.attendance.dto.SummaryDtos;
import kz.aitu.hrms.attendance.security.CurrentUser;
import kz.aitu.hrms.attendance.service.AttendanceService;
import kz.aitu.hrms.attendance.service.SummaryService;
import kz.aitu.hrms.common.dto.ApiResponse;
import kz.aitu.hrms.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "Attendance", description = "Check-in/out, records, summaries — web/manual")
@RestController
@RequestMapping("/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final SummaryService summaryService;

    // Manual / web check-in / check-out

    @Operation(summary = "Web/manual check-in (uses JWT identity by default)")
    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('ATTENDANCE_CHECKIN')")
    public ResponseEntity<ApiResponse<AttendanceDtos.CheckInResponse>> checkIn(
            @RequestBody(required = false) AttendanceDtos.ManualCheckInRequest req) {
        UUID employeeId = req != null && req.getEmployeeId() != null
                ? req.getEmployeeId()
                : CurrentUser.employeeId();
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.checkIn(
                        employeeId,
                        req == null ? null : req.getMethod(),
                        req == null ? null : req.getLocationLat(),
                        req == null ? null : req.getLocationLng())));
    }

    @Operation(summary = "Web/manual check-out (uses JWT identity by default)")
    @PostMapping("/check-out")
    @PreAuthorize("hasAuthority('ATTENDANCE_CHECKIN')")
    public ResponseEntity<ApiResponse<AttendanceDtos.CheckInResponse>> checkOut(
            @RequestBody(required = false) AttendanceDtos.ManualCheckOutRequest req) {
        UUID employeeId = req != null && req.getEmployeeId() != null
                ? req.getEmployeeId()
                : CurrentUser.employeeId();
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.checkOut(
                        employeeId,
                        req == null ? null : req.getMethod(),
                        req == null ? null : req.getLocationLat(),
                        req == null ? null : req.getLocationLng())));
    }

    @Operation(summary = "My attendance status today")
    @GetMapping("/today")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AttendanceDtos.TodayResponse>> today() {
        UUID employeeId = CurrentUser.employeeId();
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.today(employeeId)));
    }

    // Records

    @Operation(summary = "My attendance records (paginated, optional from/to)")
    @GetMapping("/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<AttendanceDtos.RecordResponse>>> ownRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 30, sort = "workDate") Pageable pageable) {
        UUID employeeId = CurrentUser.employeeId();
        if (employeeId == null) {
            throw new BusinessException("Caller has no associated employee profile");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.ownRecords(employeeId, from, to, pageable)));
    }

    @Operation(summary = "Records for a specific employee")
    @GetMapping("/records/employee/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW_ALL') or hasAuthority('ATTENDANCE_VIEW_TEAM')")
    public ResponseEntity<ApiResponse<Page<AttendanceDtos.RecordResponse>>> employeeRecords(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 30, sort = "workDate") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.employeeRecords(id, from, to, pageable)));
    }

    @Operation(summary = "Records for a department on a given day (defaults to today)")
    @GetMapping("/records/department/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW_ALL') or hasAuthority('ATTENDANCE_VIEW_TEAM')")
    public ResponseEntity<ApiResponse<List<AttendanceDtos.RecordResponse>>> departmentRecords(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.departmentRecords(id, date)));
    }

    @Operation(summary = "Company-wide records for a given day (defaults to today)")
    @GetMapping("/records/daily")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW_ALL')")
    public ResponseEntity<ApiResponse<List<AttendanceDtos.RecordResponse>>> dailyRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.dailyRecords(date)));
    }

    @Operation(summary = "Manual record entry")
    @PostMapping("/records")
    @PreAuthorize("hasAuthority('ATTENDANCE_MANAGE')")
    public ResponseEntity<ApiResponse<AttendanceDtos.RecordResponse>> manualEntry(
            @Valid @RequestBody AttendanceDtos.ManualEntryRequest req) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                attendanceService.manualEntry(req)));
    }

    @Operation(summary = "Correct an attendance record")
    @PutMapping("/records/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_MANAGE')")
    public ResponseEntity<ApiResponse<AttendanceDtos.RecordResponse>> updateRecord(
            @PathVariable UUID id,
            @Valid @RequestBody AttendanceDtos.UpdateRecordRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.update(id, req)));
    }

    @Operation(summary = "Mark all employees without a record on the given date as ABSENT")
    @PostMapping("/records/bulk-absent")
    @PreAuthorize("hasAuthority('ATTENDANCE_MANAGE')")
    public ResponseEntity<ApiResponse<AttendanceDtos.BulkAbsentResponse>> bulkAbsent(
            @Valid @RequestBody AttendanceDtos.BulkAbsentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.bulkAbsent(req.getDate())));
    }

    // Summaries

    @Operation(summary = "Monthly summary for an employee")
    @GetMapping("/summary/employee/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW_ALL') or hasAuthority('ATTENDANCE_VIEW_TEAM')")
    public ResponseEntity<ApiResponse<SummaryDtos.EmployeeSummary>> employeeSummary(
            @PathVariable UUID id,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(summaryService.employeeSummary(id, year, month)));
    }

    @Operation(summary = "Monthly summary aggregated for a department")
    @GetMapping("/summary/department/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW_ALL') or hasAuthority('ATTENDANCE_VIEW_TEAM')")
    public ResponseEntity<ApiResponse<SummaryDtos.AggregateSummary>> departmentSummary(
            @PathVariable UUID id,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(summaryService.departmentSummary(id, year, month)));
    }

    @Operation(summary = "Monthly summary aggregated company-wide")
    @GetMapping("/summary/company")
    @PreAuthorize("hasAuthority('ATTENDANCE_VIEW_ALL')")
    public ResponseEntity<ApiResponse<SummaryDtos.AggregateSummary>> companySummary(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(summaryService.companySummary(year, month)));
    }
}